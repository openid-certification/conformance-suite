package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates the request body for the notification endpoint.
 *
 * Per OID4VCI Section 11.1, the notification request contains:
 * - notification_id (REQUIRED): the notification_id from the credential response
 * - event (REQUIRED): one of credential_accepted, credential_failure, credential_deleted
 * - event_description (OPTIONAL): human-readable description
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.1">OID4VCI Section 11.1 - Notification Request</a>
 */
public class VCICreateNotificationRequest extends AbstractCondition {

	private final String event;

	public VCICreateNotificationRequest() {
		this("credential_accepted");
	}

	public VCICreateNotificationRequest(String event) {
		this.event = event;
	}

	@Override
	@PreEnvironment(strings = "notification_id")
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {

		String notificationId = env.getString("notification_id");
		if (Strings.isNullOrEmpty(notificationId)) {
			throw error("Missing notification_id for notification request");
		}

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("notification_id", notificationId);
		requestBody.addProperty("event", event);

		String requestBodyString = requestBody.toString();
		env.putString("resource_request_entity", requestBodyString);

		logSuccess("Created notification request",
			args("request_body", requestBody, "event", event, "notification_id", notificationId));

		return env;
	}
}
