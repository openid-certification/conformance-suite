package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * The conformance suite, when acting as a wallet, does not support the OID4VP
 * 1.0 transaction_data parameter. Per OID4VP §5, "Wallets that do not support
 * this parameter MUST reject requests that contain it." This condition fails
 * the test when a verifier under test includes transaction_data, both to
 * enforce that MUST and to flag verifiers that send transaction_data
 * unnecessarily.
 */
public class CheckNoTransactionDataInVpAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement transactionData = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "transaction_data");

		if (transactionData != null) {
			throw error("Authorization request contains the 'transaction_data' parameter, but the conformance"
					+ " suite (acting as a wallet) does not support transaction_data. Per OID4VP §5, wallets that"
					+ " do not support this parameter MUST reject requests that contain it.",
				args("transaction_data", transactionData));
		}

		logSuccess("Authorization request does not contain transaction_data");
		return env;
	}
}
