package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWEUtil;

public class CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted extends AbstractCreateVP1FinalIsoMdocRedirectSessionTranscript {
	@Override
	@PreEnvironment(strings = { "client_id", "nonce"}, required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {

		// read from the effective request parameters, which are populated for all request methods
		// (with url_query there is no request object, so authorization_request_object is absent)
		final JsonElement jwksEl;
		try {
			jwksEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.jwks");
		} catch (Exception e) {
			throw error("Couldn't read client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY)));
		}
		if (jwksEl == null) {
			throw error("An encrypted response was requested but client_metadata.jwks is not present in the received request.");
		}
		if (!jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks must be a JSON object", args("client_jwks", jwksEl));
		}

		JsonObject clientJwks = jwksEl.getAsJsonObject();
		// use the first usable key - note that key selection has to match VP1FinalEncryptVPResponse
		JsonObject jwkJson = JWEUtil.selectFirstUsableEncKeyJson(clientJwks);
		if (jwkJson == null) {
			throw error("No usable encryption key was found in client_metadata.jwks from the authorization request", args("authorization_request", env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY)));
		}

		String clientId = env.getString("client_id");
		String nonce =  env.getString("nonce");
		// this could be redirect_uri if response_uri isn't present, but we currently only support response_modes where response_uri is present
		String responseUri = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "response_uri");

		calculateSessionTranscript(env, jwkJson, clientId, nonce, responseUri);

		return env;
	}

}
