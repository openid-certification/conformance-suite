package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Validates that client_attestation_signing_alg_values_supported and client_attestation_pop_signing_alg_values_supported
 * are present in OAuth Authorization Server metadata when token_endpoint_auth_methods_supported contains attest_jwt_client_auth.
 *
 * Per OAuth Attestation-Based Client Authentication (draft-ietf-oauth-attestation-based-client-auth):
 * "The Authorization Server MUST include client_attestation_signing_alg_values_supported and
 * client_attestation_pop_signing_alg_values_supported in its published metadata if the
 * token_endpoint_auth_methods_supported includes attest_jwt_client_auth."
 *
 * @see <a href="https://drafts.oauth.net/draft-ietf-oauth-attestation-based-client-auth/draft-ietf-oauth-attestation-based-client-auth.html#name-authorization-server-metada">OAuth Attestation-Based Client Authentication - Authorization Server Metadata</a>
 */
public class CheckDiscEndpointClientAttestationSigningAlgValuesSupported extends AbstractCondition {

	private static final String ATTEST_JWT_CLIENT_AUTH = "attest_jwt_client_auth";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement tokenEndpointAuthMethodsSupported = env.getElementFromObject("server", "token_endpoint_auth_methods_supported");

		// Check if attest_jwt_client_auth is in token_endpoint_auth_methods_supported
		if (tokenEndpointAuthMethodsSupported == null || !tokenEndpointAuthMethodsSupported.isJsonArray()) {
			logSuccess("token_endpoint_auth_methods_supported not present or not an array, skipping client attestation signing alg checks");
			return env;
		}

		boolean hasAttestJwtClientAuth = false;
		for (JsonElement method : tokenEndpointAuthMethodsSupported.getAsJsonArray()) {
			if (method.isJsonPrimitive() && ATTEST_JWT_CLIENT_AUTH.equals(method.getAsString())) {
				hasAttestJwtClientAuth = true;
				break;
			}
		}

		if (!hasAttestJwtClientAuth) {
			logSuccess("token_endpoint_auth_methods_supported does not include attest_jwt_client_auth, skipping client attestation signing alg checks",
				args("token_endpoint_auth_methods_supported", tokenEndpointAuthMethodsSupported));
			return env;
		}

		// attest_jwt_client_auth is supported, so we must have the signing alg values
		List<String> missingFields = new java.util.ArrayList<>();

		JsonElement clientAttestationSigningAlgValues = env.getElementFromObject("server", "client_attestation_signing_alg_values_supported");
		if (clientAttestationSigningAlgValues == null) {
			missingFields.add("client_attestation_signing_alg_values_supported");
		} else if (!clientAttestationSigningAlgValues.isJsonArray() || clientAttestationSigningAlgValues.getAsJsonArray().isEmpty()) {
			throw error("client_attestation_signing_alg_values_supported must be a non-empty array",
				args("client_attestation_signing_alg_values_supported", clientAttestationSigningAlgValues));
		}

		JsonElement clientAttestationPopSigningAlgValues = env.getElementFromObject("server", "client_attestation_pop_signing_alg_values_supported");
		if (clientAttestationPopSigningAlgValues == null) {
			missingFields.add("client_attestation_pop_signing_alg_values_supported");
		} else if (!clientAttestationPopSigningAlgValues.isJsonArray() || clientAttestationPopSigningAlgValues.getAsJsonArray().isEmpty()) {
			throw error("client_attestation_pop_signing_alg_values_supported must be a non-empty array",
				args("client_attestation_pop_signing_alg_values_supported", clientAttestationPopSigningAlgValues));
		}

		if (!missingFields.isEmpty()) {
			throw error("Authorization Server metadata must include client attestation signing algorithm values when token_endpoint_auth_methods_supported includes attest_jwt_client_auth",
				args("missing_fields", missingFields,
					"token_endpoint_auth_methods_supported", tokenEndpointAuthMethodsSupported));
		}

		logSuccess("Authorization Server metadata contains required client attestation signing algorithm values",
			args("client_attestation_signing_alg_values_supported", clientAttestationSigningAlgValues,
				"client_attestation_pop_signing_alg_values_supported", clientAttestationPopSigningAlgValues));

		return env;
	}
}
