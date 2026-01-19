package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates the request body for the deferred credential endpoint.
 *
 * Per OID4VCI Section 9.1, the deferred credential request contains only the transaction_id
 * that was received from the initial credential response.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9.1">OID4VCI Section 9.1 - Deferred Credential Request</a>
 */
public class VCICreateDeferredCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "deferred_transaction_id")
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {

		String transactionId = env.getString("deferred_transaction_id");
		if (Strings.isNullOrEmpty(transactionId)) {
			throw error("Missing transaction_id for deferred credential request");
		}

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("transaction_id", transactionId);

		String requestBodyString = requestBody.toString();
		env.putString("resource_request_entity", requestBodyString);

		logSuccess("Created deferred credential request",
			args("request_body", requestBody));

		return env;
	}
}
