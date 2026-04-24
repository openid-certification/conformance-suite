package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that the client attestation JWT's 'sub' claim is present and equals the
 * 'iss' claim of the associated proof-of-possession JWT. Per OAuth 2.0
 * Attestation-Based Client Authentication §5.1, 'sub' MUST be the OAuth client_id; per
 * §5.2, the PoP JWT's 'iss' MUST also be the OAuth client_id. The two MUST therefore be
 * equal. Comparing them directly avoids coupling to any server-side client-selection
 * state (e.g. multi-client mapping) and catches wallets that cross-wire an attestation
 * for one client with a PoP from another.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationSubject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_attestation_object", "client_attestation_pop_object"})
	public Environment evaluate(Environment env) {

		JsonElement subEl = env.getElementFromObject("client_attestation_object", "claims.sub");

		if (subEl == null) {
			throw error("Couldn't find sub claim in the client_attestation");
		}

		String actualSub = OIDFJSON.getString(subEl);

		JsonElement popIssEl = env.getElementFromObject("client_attestation_pop_object", "claims.iss");
		if (popIssEl == null) {
			throw error("Couldn't find iss claim in the client_attestation_pop");
		}

		String popIss = OIDFJSON.getString(popIssEl);

		if (!popIss.equals(actualSub)) {
			throw error("Client attestation sub does not match client attestation PoP iss",
				args("attestation_sub", actualSub, "pop_iss", popIss));
		}

		logSuccess("Client attestation sub matches PoP iss",
			args("attestation_sub", actualSub, "pop_iss", popIss));

		return env;
	}
}
