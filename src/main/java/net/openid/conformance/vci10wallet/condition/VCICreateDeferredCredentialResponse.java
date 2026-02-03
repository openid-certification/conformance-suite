package net.openid.conformance.vci10wallet.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Creates a deferred credential response with transaction_id per OID4VCI Section 9.
 *
 * When the Credential Issuer is unable to immediately issue the Credential, it returns
 * a response containing a transaction_id that the wallet must use to poll the deferred
 * credential endpoint.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9">OID4VCI Section 9 - Deferred Credential Issuance</a>
 */
public class VCICreateDeferredCredentialResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"credential_endpoint_response", "credential_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");

		// Generate a transaction_id for the deferred credential
		String transactionId = RandomStringUtils.secure().nextAlphanumeric(32);

		JsonObject response = new JsonObject();
		response.addProperty("transaction_id", transactionId);

		// Store the transaction_id for later validation at the deferred endpoint
		env.putString("deferred_transaction_id", transactionId);

		// Store information needed to issue the credential later
		// The credential itself and proof information should already be in the environment
		// from the credential endpoint validation

		logSuccess("Created deferred credential response with transaction_id",
			args("credential_endpoint_response", response,
				"credential_endpoint_response_headers", headers,
				"transaction_id", transactionId));

		env.putObject("credential_endpoint_response", response);
		env.putObject("credential_endpoint_response_headers", headers);

		return env;
	}
}
