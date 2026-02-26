package net.openid.conformance.vci10wallet.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10wallet.VCIErrorResponseUtil;

/**
 * Validates a deferred credential request per OID4VCI Section 9.
 *
 * The wallet must send a POST request with the transaction_id received from
 * the initial credential response.
 *
 * If validation fails, this condition sets vci.credential_error_response with the error details
 * instead of throwing, allowing the caller to return an appropriate HTTP error response.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9.1">OID4VCI Section 9.1 - Deferred Credential Request</a>
 */
public class VCIValidateDeferredCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonElement bodyJsonEl = env.getElementFromObject("incoming_request", "body_json");
		if (bodyJsonEl == null || !bodyJsonEl.isJsonObject()) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_request", "Request body is missing or not valid JSON");
			throw error("Request body is missing or not valid JSON");
		}

		JsonObject requestBodyJson = bodyJsonEl.getAsJsonObject();

		// Check for transaction_id in the request
		if (!requestBodyJson.has("transaction_id")) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_request", "Deferred credential request must contain transaction_id");
			throw error("Deferred credential request must contain transaction_id",
				args("request_body", requestBodyJson));
		}

		String requestTransactionId = OIDFJSON.getString(requestBodyJson.get("transaction_id"));
		if (Strings.isNullOrEmpty(requestTransactionId)) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_request", "transaction_id in deferred credential request must not be empty");
			throw error("transaction_id in deferred credential request must not be empty",
				args("request_body", requestBodyJson));
		}

		// Validate that the transaction_id matches the one we issued
		String expectedTransactionId = env.getString("deferred_transaction_id");
		if (Strings.isNullOrEmpty(expectedTransactionId)) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_transaction_id", "No deferred credential issuance is pending");
			throw error("No deferred transaction_id found in environment - no deferred credential issuance is pending");
		}

		if (!requestTransactionId.equals(expectedTransactionId)) {
			VCIErrorResponseUtil.setErrorResponse(env, "invalid_transaction_id", "The transaction_id is not valid or has expired");
			throw error("transaction_id in deferred credential request does not match the issued transaction_id",
				args("expected_transaction_id", expectedTransactionId,
					"received_transaction_id", requestTransactionId));
		}

		logSuccess("Deferred credential request is valid",
			args("transaction_id", requestTransactionId));

		return env;
	}

}
