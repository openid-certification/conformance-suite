package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Per OID4VP 1.0 Final §5.10, when the wallet POSTs a {@code wallet_nonce} value to the
 * verifier's {@code request_uri}, the verifier MUST include {@code wallet_nonce} as a top-level
 * claim in the returned Request Object whose value matches the value the wallet sent.
 */
public class EnsureWalletNonceClaimMatchesPostedValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object", strings = "wallet_nonce")
	public Environment evaluate(Environment env) {

		String posted = env.getString("wallet_nonce");

		JsonElement claim = env.getElementFromObject("authorization_request_object", "claims.wallet_nonce");
		if (claim == null) {
			throw error("Request object is missing the wallet_nonce claim. Per OID4VP §5.10, when the wallet POSTs a wallet_nonce, the verifier MUST include it as a top-level claim in the returned Request Object.",
				args("expected_wallet_nonce", posted));
		}
		if (!claim.isJsonPrimitive() || !claim.getAsJsonPrimitive().isString()) {
			throw error("wallet_nonce claim in request object is not a string",
				args("wallet_nonce", claim, "expected_wallet_nonce", posted));
		}

		String returned = OIDFJSON.getString(claim);
		if (!posted.equals(returned)) {
			throw error("wallet_nonce claim in request object does not match the value the wallet POSTed",
				args("wallet_nonce_returned", returned, "wallet_nonce_posted", posted));
		}

		logSuccess("wallet_nonce claim in request object matches the value the wallet POSTed",
			args("wallet_nonce", posted));
		return env;
	}
}
