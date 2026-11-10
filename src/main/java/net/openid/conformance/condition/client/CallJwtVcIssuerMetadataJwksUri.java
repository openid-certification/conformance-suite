package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * Fetches the JWK set referenced by {@code jwks_uri} in the JWT VC Issuer Metadata and stores the
 * full response as {@code jwt_vc_issuer_metadata_jwks_uri_endpoint_response} for the status check
 * and {@link ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse}.
 */
public class CallJwtVcIssuerMetadataJwksUri extends AbstractCallEndpointWithGet {

	public static final String RESPONSE_KEY = "jwt_vc_issuer_metadata_jwks_uri_endpoint_response";

	@Override
	@PreEnvironment(strings = "jwt_vc_issuer_metadata_jwks_uri")
	public Environment evaluate(Environment env) {
		return callEndpointWithGet(env, new IgnoreErrorsErrorHandler(),
			List.of(MediaType.parseMediaType("application/jwk-set+json"), MediaType.APPLICATION_JSON),
			env.getString("jwt_vc_issuer_metadata_jwks_uri"), "JWT VC Issuer Metadata jwks_uri", RESPONSE_KEY);
	}
}
