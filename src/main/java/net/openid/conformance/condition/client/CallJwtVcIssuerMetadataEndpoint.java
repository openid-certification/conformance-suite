package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * Fetches the JWT VC Issuer Metadata located by {@link ResolveStatusListTokenKeyFromJwtOrCredential}
 * and stores the full response as {@code jwt_vc_issuer_metadata_endpoint_response} for the
 * status, content type and content checks that follow.
 *
 * Publishing JWT VC Issuer Metadata is optional, so a 404 is logged and no response is stored;
 * key resolution then continues with the next source.
 */
public class CallJwtVcIssuerMetadataEndpoint extends AbstractCallEndpointWithGet {

	public static final String RESPONSE_KEY = "jwt_vc_issuer_metadata_endpoint_response";

	@Override
	@PreEnvironment(required = "status_list_token_jwt_vc_issuer")
	public Environment evaluate(Environment env) {
		String metadataUrl = env.getString("status_list_token_jwt_vc_issuer", "metadata_url");

		callEndpointWithGet(env, new IgnoreErrorsErrorHandler(), List.of(MediaType.APPLICATION_JSON),
			metadataUrl, "JWT VC Issuer Metadata", RESPONSE_KEY);

		if (env.getInteger(RESPONSE_KEY, "status") == HttpStatus.NOT_FOUND.value()) {
			env.removeObject(RESPONSE_KEY);
			log("No JWT VC Issuer Metadata is published for the issuer",
				args("iss", env.getString("status_list_token_jwt_vc_issuer", "iss"), "url", metadataUrl));
		}
		return env;
	}
}
