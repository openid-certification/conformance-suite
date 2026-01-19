package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds client_attestation_signing_alg_values_supported and client_attestation_pop_signing_alg_values_supported
 * to the OAuth Authorization Server metadata.
 *
 * Per OAuth Attestation-Based Client Authentication (draft-ietf-oauth-attestation-based-client-auth):
 * "The Authorization Server MUST include client_attestation_signing_alg_values_supported and
 * client_attestation_pop_signing_alg_values_supported in its published metadata if the
 * token_endpoint_auth_methods_supported includes attest_jwt_client_auth."
 *
 * @see <a href="https://drafts.oauth.net/draft-ietf-oauth-attestation-based-client-auth/draft-ietf-oauth-attestation-based-client-auth.html#name-authorization-server-metada">OAuth Attestation-Based Client Authentication - Authorization Server Metadata</a>
 */
public class AddClientAttestationSigningAlgValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		// Per HAIP, ES256 is the required algorithm
		JsonArray clientAttestationSigningAlgValuesSupported = new JsonArray();
		clientAttestationSigningAlgValuesSupported.add("ES256");

		JsonArray clientAttestationPopSigningAlgValuesSupported = new JsonArray();
		clientAttestationPopSigningAlgValuesSupported.add("ES256");

		server.add("client_attestation_signing_alg_values_supported", clientAttestationSigningAlgValuesSupported);
		server.add("client_attestation_pop_signing_alg_values_supported", clientAttestationPopSigningAlgValuesSupported);

		log("Added client attestation signing algorithm values to OAuth server metadata",
			args("client_attestation_signing_alg_values_supported", clientAttestationSigningAlgValuesSupported,
				"client_attestation_pop_signing_alg_values_supported", clientAttestationPopSigningAlgValuesSupported));

		return env;
	}
}
