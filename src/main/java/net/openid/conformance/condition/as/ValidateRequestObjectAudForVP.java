package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the request object aud claim for VP verifier tests.
 *
 * Per OID4VP 1.0 Final section 5.8, the verifier sets aud in the request object to either:
 *   - "https://self-issued.me/v2" when Static Discovery metadata is used; or
 *   - the wallet's issuer value, when Dynamic Discovery is performed.
 *
 * The conformance suite (acting as the wallet) publishes its issuer URL via the generated
 * server configuration, so we accept either the symbolic self-issued value or that issuer.
 */
public class ValidateRequestObjectAudForVP extends AbstractCondition {

	private static final String SELF_ISSUED_V2 = "https://self-issued.me/v2";

	@Override
	@PreEnvironment(required = { "authorization_request_object", "server" })
	public Environment evaluate(Environment env) {
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		String walletIssuer = env.getString("server", "issuer");
		if (aud == null) {
			throw error("Missing aud claim in request object",
				args("expected_self_issued", SELF_ISSUED_V2, "expected_wallet_issuer", walletIssuer));
		}

		if (aud.isJsonArray()) {
			boolean hasSelfIssued = aud.getAsJsonArray().contains(new JsonPrimitive(SELF_ISSUED_V2));
			boolean hasWalletIssuer = walletIssuer != null && aud.getAsJsonArray().contains(new JsonPrimitive(walletIssuer));
			if (!hasSelfIssued && !hasWalletIssuer) {
				throw error("aud claim array does not contain either 'https://self-issued.me/v2' or the wallet's issuer URL",
					args("aud", aud, "expected_self_issued", SELF_ISSUED_V2, "expected_wallet_issuer", walletIssuer));
			}
		} else {
			String audStr = OIDFJSON.getString(aud);
			if (!SELF_ISSUED_V2.equals(audStr) && !audStr.equals(walletIssuer)) {
				throw error("aud claim does not match 'https://self-issued.me/v2' or the wallet's issuer URL",
					args("aud", aud, "expected_self_issued", SELF_ISSUED_V2, "expected_wallet_issuer", walletIssuer));
			}
		}

		logSuccess("Request object aud claim is valid",
			args("aud", aud, "expected_self_issued", SELF_ISSUED_V2, "expected_wallet_issuer", walletIssuer));
		return env;
	}
}
