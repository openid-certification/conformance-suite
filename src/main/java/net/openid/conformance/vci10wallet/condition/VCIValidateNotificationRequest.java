package net.openid.conformance.vci10wallet.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

/**
 * Validates an incoming notification request per OID4VCI Section 11.1.
 *
 * The notification request body must contain:
 * - notification_id (REQUIRED): string matching the one sent in the credential response
 * - event (REQUIRED): one of credential_accepted, credential_failure, credential_deleted
 * - event_description (OPTIONAL): string with characters restricted to %x20-21 / %x23-5B / %x5D-7E
 *
 * If validation fails, this condition sets vci.notification_error_response with the error details
 * per Section 11.3, allowing the caller to return an HTTP 400 error response.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.1">OID4VCI Section 11.1 - Notification Request</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.3">OID4VCI Section 11.3 - Notification Error Response</a>
 */
public class VCIValidateNotificationRequest extends AbstractCondition {

	private static final Set<String> VALID_EVENTS = Set.of(
		"credential_accepted",
		"credential_failure",
		"credential_deleted"
	);

	@Override
	@PreEnvironment(required = "incoming_request", strings = "notification_id")
	public Environment evaluate(Environment env) {

		JsonElement bodyJsonEl = env.getElementFromObject("incoming_request", "body_json");
		if (bodyJsonEl == null || !bodyJsonEl.isJsonObject()) {
			setErrorResponse(env, "invalid_notification_request", "Request body is missing or not a JSON object");
			throw error("Notification request body is missing or not a JSON object");
		}

		JsonObject body = bodyJsonEl.getAsJsonObject();

		// Validate notification_id
		JsonElement notificationIdEl = body.get("notification_id");
		if (notificationIdEl == null) {
			setErrorResponse(env, "invalid_notification_request", "Missing required notification_id field");
			throw error("Notification request is missing required 'notification_id' field",
				args("request_body", body));
		}
		String notificationId = OIDFJSON.getString(notificationIdEl);
		if (Strings.isNullOrEmpty(notificationId)) {
			setErrorResponse(env, "invalid_notification_id", "The notification_id is empty");
			throw error("Notification request 'notification_id' is empty",
				args("request_body", body));
		}

		String expectedNotificationId = env.getString("notification_id");
		if (!notificationId.equals(expectedNotificationId)) {
			setErrorResponse(env, "invalid_notification_id",
				"The notification_id does not match any issued notification_id");
			throw error("Notification request 'notification_id' does not match the one sent in the credential response",
				args("expected", expectedNotificationId, "actual", notificationId));
		}

		// Validate event
		JsonElement eventEl = body.get("event");
		if (eventEl == null) {
			setErrorResponse(env, "invalid_notification_request", "Missing required event field");
			throw error("Notification request is missing required 'event' field",
				args("request_body", body));
		}
		String event = OIDFJSON.getString(eventEl);
		if (!VALID_EVENTS.contains(event)) {
			setErrorResponse(env, "invalid_notification_request", "Invalid event value: " + event);
			throw error("Notification request 'event' has an invalid value",
				args("event", event, "valid_events", VALID_EVENTS));
		}

		// Validate event_description if present
		// Per spec: values MUST NOT include characters outside %x20-21 / %x23-5B / %x5D-7E
		// (printable ASCII excluding double-quote and backslash)
		JsonElement eventDescriptionEl = body.get("event_description");
		if (eventDescriptionEl != null) {
			if (!eventDescriptionEl.isJsonPrimitive() || !eventDescriptionEl.getAsJsonPrimitive().isString()) {
				setErrorResponse(env, "invalid_notification_request",
					"event_description must be a string");
				throw error("Notification request 'event_description' must be a string",
					args("event_description", eventDescriptionEl));
			}
			String eventDescription = OIDFJSON.getString(eventDescriptionEl);
			for (int i = 0; i < eventDescription.length(); i++) {
				char c = eventDescription.charAt(i);
				if (!isAllowedEventDescriptionChar(c)) {
					setErrorResponse(env, "invalid_notification_request",
						"event_description contains invalid character");
					throw error("Notification request 'event_description' contains invalid character at index " + i,
						args("event_description", eventDescription, "invalid_char", String.valueOf(c),
							"char_code", String.format("0x%02X", (int) c)));
				}
			}
		}

		logSuccess("Notification request is valid",
			args("notification_id", notificationId, "event", event));

		return env;
	}

	/**
	 * Checks if a character is in the allowed set for event_description per OID4VCI Section 11.1:
	 * %x20-21 / %x23-5B / %x5D-7E (printable ASCII excluding double-quote 0x22 and backslash 0x5C)
	 */
	private static boolean isAllowedEventDescriptionChar(char c) {
		return (c >= 0x20 && c <= 0x21)
			|| (c >= 0x23 && c <= 0x5B)
			|| (c >= 0x5D && c <= 0x7E);
	}

	private void setErrorResponse(Environment env, String errorCode, String errorDescription) {
		JsonObject errorBody = new JsonObject();
		errorBody.addProperty("error", errorCode);
		errorBody.addProperty("error_description", errorDescription);

		JsonObject errorResponse = new JsonObject();
		errorResponse.add("body", errorBody);

		env.putObject("vci", "notification_error_response", errorResponse);
	}
}
