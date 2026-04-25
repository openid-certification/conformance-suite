package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Per OID4VP 1.0 Final §5.10, {@code wallet_nonce} is a claim only included by the verifier when
 * the wallet POSTs a {@code wallet_nonce} parameter to the verifier's {@code request_uri}. When
 * the wallet did not POST one (i.e. the wallet used GET, or did not include the parameter), the
 * Request Object MUST NOT contain a {@code wallet_nonce} claim.
 */
public class EnsureNoWalletNonceInRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		JsonElement claim = env.getElementFromObject("authorization_request_object", "claims.wallet_nonce");
		if (claim != null) {
			throw error("Request object contains a wallet_nonce claim, but the wallet did not POST a wallet_nonce. Per OID4VP §5.10 the verifier should only include this claim in response to a wallet that POSTed wallet_nonce.",
				args("wallet_nonce", claim));
		}
		logSuccess("Request object does not contain a wallet_nonce claim, as expected for non-POST flows");
		return env;
	}
}
