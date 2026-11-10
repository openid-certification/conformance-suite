package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Extracts the JWK set from a 200 response of the JWT VC Issuer Metadata {@code jwks_uri} and
 * stores it as {@code jwt_vc_issuer_metadata_jwks}.
 *
 * Any other HTTP status is reported by the status code check; the response is then not used.
 */
public class ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY, strings = "jwt_vc_issuer_metadata_jwks_uri")
	public Environment evaluate(Environment env) {
		String responseKey = CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY;
		String jwksUri = env.getString("jwt_vc_issuer_metadata_jwks_uri");

		int status = env.getInteger(responseKey, "status");
		if (status != 200) {
			log("The jwks_uri response is not used because it was not a 200 response",
				args("jwks_uri", jwksUri, "http_status", status));
			return env;
		}

		JsonElement body = env.getElementFromObject(responseKey, "body_json");
		if (body == null || !body.isJsonObject()) {
			throw error("The response from the jwks_uri in the JWT VC Issuer Metadata is not a JSON object",
				args("jwks_uri", jwksUri, "body", env.getElementFromObject(responseKey, "body")));
		}

		env.putObject("jwt_vc_issuer_metadata_jwks", body.getAsJsonObject());
		logSuccess("Fetched the issuer keys from the jwks_uri in the JWT VC Issuer Metadata",
			args("jwks_uri", jwksUri, "jwks", body));
		return env;
	}
}
