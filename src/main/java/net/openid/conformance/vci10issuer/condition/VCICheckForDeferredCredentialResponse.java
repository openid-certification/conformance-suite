package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks if the credential endpoint response is a deferred response (contains transaction_id).
 *
 * Per OID4VCI Section 9, if the Credential Issuer is unable to immediately issue the Credential,
 * it responds with HTTP 202 Accepted and a transaction_id that the wallet must use to poll
 * the deferred credential endpoint.
 *
 * This condition sets "deferred_credential_response" to true/false in the environment.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9">OID4VCI Section 9 - Deferred Credential Issuance</a>
 */
public class VCICheckForDeferredCredentialResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	@PostEnvironment(strings = "deferred_credential_response")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response").getAsJsonObject();
		JsonObject credentialResponseBodyJson = JsonParser.parseString(OIDFJSON.getString(endpointResponse.get("body"))).getAsJsonObject();

		// Check HTTP status code - deferred responses use 202 Accepted
		JsonElement statusCodeEl = endpointResponse.get("status_code");
		Integer statusCode = statusCodeEl != null ? OIDFJSON.getInt(statusCodeEl) : null;

		// Check for transaction_id in response
		JsonElement transactionIdEl = credentialResponseBodyJson.get("transaction_id");
		boolean hasTransactionId = transactionIdEl != null && transactionIdEl.isJsonPrimitive() && transactionIdEl.getAsJsonPrimitive().isString();

		// Check for credentials in response (indicates immediate issuance)
		JsonElement credentialsEl = credentialResponseBodyJson.get("credentials");
		boolean hasCredentials = credentialsEl != null && credentialsEl.isJsonArray();

		boolean isDeferred = false;

		if (hasTransactionId && !hasCredentials) {
			// This is a deferred response
			isDeferred = true;
			String transactionId = OIDFJSON.getString(transactionIdEl);
			env.putString("deferred_transaction_id", transactionId);

			// Per spec, deferred responses should use HTTP 202
			if (statusCode != null && statusCode != 202) {
				log("Deferred credential response should use HTTP 202 Accepted, got " + statusCode,
					args("status_code", statusCode, "transaction_id", transactionId));
			}

			logSuccess("Credential response is deferred, received transaction_id",
				args("transaction_id", transactionId, "status_code", statusCode));
		} else if (hasCredentials) {
			logSuccess("Credential response is immediate, contains credentials",
				args("status_code", statusCode));
		} else {
			throw error("Credential response contains neither credentials nor transaction_id",
				args("credential_response", credentialResponseBodyJson));
		}

		env.putString("deferred_credential_response", String.valueOf(isDeferred));

		return env;
	}
}
