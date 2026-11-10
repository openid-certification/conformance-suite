package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks that the JWT VC Issuer Metadata contains exactly one of {@code jwks} and
 * {@code jwks_uri}. An inline {@code jwks} is stored as {@code jwt_vc_issuer_metadata_jwks};
 * a {@code jwks_uri} is stored as the string {@code jwt_vc_issuer_metadata_jwks_uri} for
 * {@link CallJwtVcIssuerMetadataJwksUri}. Neither is stored when the check fails.
 */
public class ExtractJwksFromJwtVcIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwt_vc_issuer_metadata")
	public Environment evaluate(Environment env) {
		JsonObject metadata = env.getObject("jwt_vc_issuer_metadata");
		JsonElement jwksEl = metadata.get("jwks");
		JsonElement jwksUriEl = metadata.get("jwks_uri");

		if (jwksEl != null && jwksUriEl != null) {
			throw error("The JWT VC Issuer Metadata contains both 'jwks' and 'jwks_uri', but must contain only one of them",
				args("metadata", metadata));
		}
		if (jwksEl == null && jwksUriEl == null) {
			throw error("The JWT VC Issuer Metadata contains neither 'jwks' nor 'jwks_uri'",
				args("metadata", metadata));
		}

		if (jwksEl != null) {
			if (!jwksEl.isJsonObject()) {
				throw error("'jwks' in the JWT VC Issuer Metadata is not a JSON object", args("jwks", jwksEl));
			}
			env.putObject("jwt_vc_issuer_metadata_jwks", jwksEl.getAsJsonObject());
			logSuccess("The JWT VC Issuer Metadata contains the issuer keys inline", args("jwks", jwksEl));
			return env;
		}

		if (!jwksUriEl.isJsonPrimitive() || !jwksUriEl.getAsJsonPrimitive().isString()) {
			throw error("'jwks_uri' in the JWT VC Issuer Metadata is not a string", args("jwks_uri", jwksUriEl));
		}
		String jwksUri = OIDFJSON.getString(jwksUriEl);
		env.putString("jwt_vc_issuer_metadata_jwks_uri", jwksUri);
		logSuccess("The JWT VC Issuer Metadata references the issuer keys by jwks_uri", args("jwks_uri", jwksUri));
		return env;
	}
}
