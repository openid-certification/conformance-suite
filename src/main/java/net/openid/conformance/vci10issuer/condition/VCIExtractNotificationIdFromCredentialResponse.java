package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Extracts notification_id from the credential endpoint response.
 *
 * Per OID4VCI Section 8.3, when the credential issuer metadata includes a notification_endpoint,
 * the credential response may contain a notification_id string that the wallet should use
 * when sending a notification to the issuer.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3">OID4VCI Section 8.3 - Credential Response</a>
 */
public class VCIExtractNotificationIdFromCredentialResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject credentialResponseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();

		JsonElement notificationIdEl = credentialResponseBodyJson.get("notification_id");
		if (notificationIdEl == null) {
			log("Credential response does not contain a notification_id field.");
			return env;
		}

		if (!notificationIdEl.isJsonPrimitive() || !notificationIdEl.getAsJsonPrimitive().isString()) {
			throw error("notification_id field in credential response is not a string.",
				args("credential_response", credentialResponseBodyJson));
		}

		String notificationId = OIDFJSON.getString(notificationIdEl);

		log("Extracted notification_id from credential response",
			args("notification_id", notificationId));

		env.putString("notification_id", notificationId);

		return env;
	}
}
