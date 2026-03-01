package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that a deferred credential response contains the required 'interval' field.
 *
 * Per OID4VCI Section 9.3, the 'interval' field is REQUIRED when 'transaction_id' is present
 * in the credential response. It specifies the minimum amount of time in seconds that the
 * Wallet needs to wait between polling requests to the Deferred Credential Endpoint.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9.3">OID4VCI Section 9.3</a>
 */
public class VCIEnsureIntervalPresentInDeferredResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject credentialResponseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();

		JsonElement intervalEl = credentialResponseBodyJson.get("interval");
		if (intervalEl == null) {
			throw error("Deferred credential response is missing the required 'interval' field. " +
				"Per OID4VCI Section 9.3, 'interval' is REQUIRED when 'transaction_id' is present.",
				args("credential_response", credentialResponseBodyJson));
		}

		if (!intervalEl.isJsonPrimitive() || !intervalEl.getAsJsonPrimitive().isNumber()) {
			throw error("'interval' field in deferred credential response must be a number",
				args("interval", intervalEl, "credential_response", credentialResponseBodyJson));
		}

		logSuccess("Deferred credential response contains required 'interval' field",
			args("interval", intervalEl));

		return env;
	}
}
