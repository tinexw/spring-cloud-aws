/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 */
public class TopicMessageChannelTest {

	@Test
	public void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content")
				.setHeader(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, "Subject")
				.build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSns, only()).publish(
				PublishRequest.builder().topicArn("topicArn").message("Message content")
						.subject("Subject").messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	public void sendMessage_validTextMessageWithoutSubject_returnsTrue()
			throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content")
				.build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSns, only()).publish(
				PublishRequest.builder().topicArn("topicArn").message("Message content")
						.subject(null).messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	public void sendMessage_validTextMessageAndTimeout_timeoutIsIgnored()
			throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content")
				.build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage, 10);

		// Assert
		verify(amazonSns, only()).publish(
				PublishRequest.builder().topicArn("topicArn").message("Message content")
						.subject(null).messageAttributes(isNotNull()).build());
		assertThat(sent).isTrue();
	}

	@Test
	public void sendMessage_withStringMessageHeader_shouldBeSentAsTopicMessageAttribute()
			throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor
				.forClass(PublishRequest.class);
		when(amazonSns.publish(publishRequestArgumentCaptor.capture()))
				.thenReturn(PublishResponse.builder().build());

		String headerValue = "Header value";
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(headerName, headerValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes()
				.get(headerName).stringValue()).isEqualTo(headerValue);
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes()
				.get(headerName).dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
	}

	@Test
	public void sendMessage_withNumericMessageHeaders_shouldBeSentAsTopicMessageAttributes()
			throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor
				.forClass(PublishRequest.class);
		when(amazonSns.publish(publishRequestArgumentCaptor.capture()))
				.thenReturn(PublishResponse.builder().build());

		double doubleValue = 1234.56;
		long longValue = 1234L;
		int integerValue = 1234;
		byte byteValue = 2;
		short shortValue = 12;
		float floatValue = 1234.56f;
		BigInteger bigIntegerValue = new BigInteger("616416546156");
		BigDecimal bigDecimalValue = new BigDecimal("7834938");

		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader("double", doubleValue).setHeader("long", longValue)
				.setHeader("integer", integerValue).setHeader("byte", byteValue)
				.setHeader("short", shortValue).setHeader("float", floatValue)
				.setHeader("bigInteger", bigIntegerValue)
				.setHeader("bigDecimal", bigDecimalValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		Map<String, MessageAttributeValue> messageAttributes = publishRequestArgumentCaptor
				.getValue().messageAttributes();
		assertThat(messageAttributes.get("double").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Double");
		assertThat(messageAttributes.get("double").stringValue())
				.isEqualTo(String.valueOf(doubleValue));
		assertThat(messageAttributes.get("long").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Long");
		assertThat(messageAttributes.get("long").stringValue())
				.isEqualTo(String.valueOf(longValue));
		assertThat(messageAttributes.get("integer").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Integer");
		assertThat(messageAttributes.get("integer").stringValue())
				.isEqualTo(String.valueOf(integerValue));
		assertThat(messageAttributes.get("byte").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Byte");
		assertThat(messageAttributes.get("byte").stringValue())
				.isEqualTo(String.valueOf(byteValue));
		assertThat(messageAttributes.get("short").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Short");
		assertThat(messageAttributes.get("short").stringValue())
				.isEqualTo(String.valueOf(shortValue));
		assertThat(messageAttributes.get("float").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.lang.Float");
		assertThat(messageAttributes.get("float").stringValue())
				.isEqualTo(String.valueOf(floatValue));
		assertThat(messageAttributes.get("bigInteger").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigInteger");
		assertThat(messageAttributes.get("bigInteger").stringValue())
				.isEqualTo(String.valueOf(bigIntegerValue));
		assertThat(messageAttributes.get("bigDecimal").dataType())
				.isEqualTo(MessageAttributeDataTypes.NUMBER + ".java.math.BigDecimal");
		assertThat(messageAttributes.get("bigDecimal").stringValue())
				.isEqualTo(String.valueOf(bigDecimalValue));
	}

	@Test
	public void sendMessage_withBinaryMessageHeader_shouldBeSentAsBinaryMessageAttribute()
			throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		ArgumentCaptor<PublishRequest> publishRequestArgumentCaptor = ArgumentCaptor
				.forClass(PublishRequest.class);
		when(amazonSns.publish(publishRequestArgumentCaptor.capture()))
				.thenReturn(PublishResponse.builder().build());

		ByteBuffer headerValue = ByteBuffer.wrap("My binary data!".getBytes());
		String headerName = "MyHeader";
		Message<String> message = MessageBuilder.withPayload("Hello")
				.setHeader(headerName, headerValue).build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes()
				.get(headerName).binaryValue().asByteBuffer()).isEqualTo(headerValue);
		assertThat(publishRequestArgumentCaptor.getValue().messageAttributes()
				.get(headerName).dataType()).isEqualTo(MessageAttributeDataTypes.BINARY);
	}

	@Test
	public void sendMessage_withUuidAsId_shouldConvertUuidToString() throws Exception {
		// Arrange
		SnsClient amazonSns = mock(SnsClient.class);
		TopicMessageChannel messageChannel = new TopicMessageChannel(amazonSns,
				"http://testQueue");
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		UUID uuid = (UUID) message.getHeaders().get(MessageHeaders.ID);

		ArgumentCaptor<PublishRequest> sendMessageRequestArgumentCaptor = ArgumentCaptor
				.forClass(PublishRequest.class);
		when(amazonSns.publish(sendMessageRequestArgumentCaptor.capture()))
				.thenReturn(PublishResponse.builder().build());

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		assertThat(sendMessageRequestArgumentCaptor.getValue().messageAttributes()
				.get(MessageHeaders.ID).stringValue()).isEqualTo(uuid.toString());
	}

}
