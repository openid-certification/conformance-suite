package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted extends AbstractCreateVP1FinalIsoMdocRedirectSessionTranscript {
	@Override
	@PreEnvironment(strings = { "client_id", "nonce"}, required = "authorization_request_object" )
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {

		final JsonElement jwksEl;
		try {
			jwksEl = env.getElementFromObject("authorization_request_object", "claims.client_metadata.jwks");
		} catch (Exception e) {
			throw error("Couldn't read client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject("authorization_request_object")));
		}
		if (jwksEl == null) {
			throw error("An encrypted response was requested but client_metadata.jwks is not present in the received request.");
		}
		if (!jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks must be a JSON object", args("client_jwks", jwksEl));
		}

		JsonObject clientJwks = jwksEl.getAsJsonObject();
		// use the first key for now - note that key selection has to match VP1FinalEncryptVPResponse
		JsonObject jwkJson;
		try {
			jwkJson = clientJwks.get("keys").getAsJsonArray().get(0).getAsJsonObject();
		} catch (Exception e) {
			throw error("Couldn't read first key in client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject("authorization_request_object")));
		}

		String clientId = env.getString("client_id");
		String nonce =  env.getString("nonce");
		// this could be redirect_uri if response_uri isn't present, but we currently only support response_modes where response_uri is present
		String responseUri = env.getString("authorization_request_object", "claims.response_uri");

		calculateSessionTranscript(env, jwkJson, clientId, nonce, responseUri);

		return env;
	}

}
