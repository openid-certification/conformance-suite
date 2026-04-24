package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates that the client attestation JWT's 'sub' claim is present, equals the
 * 'iss' claim of the associated proof-of-possession JWT, and matches the selected
 * OAuth client_id for this request. Per OAuth 2.0 Attestation-Based Client
 * Authentication §5.1, 'sub' MUST be the OAuth client_id; per §5.2, the PoP JWT's
 * 'iss' MUST also be the OAuth client_id.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationSubject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_attestation_object", "client_attestation_pop_object", "client"})
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

		String expectedClientId = env.getString("client", "client_id");
		if (expectedClientId == null) {
			throw error("Couldn't find client_id for the selected client");
		}

		if (!expectedClientId.equals(actualSub)) {
			throw error("Client attestation sub does not match the selected client_id",
				args("expected_client_id", expectedClientId, "attestation_sub", actualSub, "pop_iss", popIss));
		}

		logSuccess("Client attestation sub matches PoP iss and selected client_id",
			args("expected_client_id", expectedClientId, "attestation_sub", actualSub, "pop_iss", popIss));

		return env;
	}
}
