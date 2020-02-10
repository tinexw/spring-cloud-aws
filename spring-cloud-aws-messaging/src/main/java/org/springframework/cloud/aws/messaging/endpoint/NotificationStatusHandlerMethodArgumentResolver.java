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

package org.springframework.cloud.aws.messaging.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;

/**
 * @author Agim Emruli
 */
public class NotificationStatusHandlerMethodArgumentResolver
		extends AbstractNotificationMessageHandlerMethodArgumentResolver {

	private final SnsClient amazonSns;

	public NotificationStatusHandlerMethodArgumentResolver(SnsClient amazonSns) {
		this.amazonSns = amazonSns;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return NotificationStatus.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	protected Object doResolveArgumentFromNotificationMessage(JsonNode content,
			HttpInputMessage request, Class<?> parameterType) {
		if (!"SubscriptionConfirmation".equals(content.get("Type").asText())
				&& !"UnsubscribeConfirmation".equals(content.get("Type").asText())) {
			throw new IllegalArgumentException(
					"NotificationStatus is only available for subscription and unsubscription requests");
		}
		return new AmazonSnsNotificationStatus(this.amazonSns,
				content.get("TopicArn").asText(), content.get("Token").asText());
	}

	private static final class AmazonSnsNotificationStatus implements NotificationStatus {

		private final SnsClient amazonSns;

		private final String topicArn;

		private final String confirmationToken;

		private AmazonSnsNotificationStatus(SnsClient amazonSns, String topicArn,
				String confirmationToken) {
			this.amazonSns = amazonSns;
			this.topicArn = topicArn;
			this.confirmationToken = confirmationToken;
		}

		@Override
		public void confirmSubscription() {
			this.amazonSns.confirmSubscription(ConfirmSubscriptionRequest.builder()
					.topicArn(this.topicArn).token(this.confirmationToken).build());
		}

	}

}
