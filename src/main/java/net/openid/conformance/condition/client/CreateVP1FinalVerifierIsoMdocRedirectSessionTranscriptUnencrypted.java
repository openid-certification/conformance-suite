package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted extends AbstractCreateVP1FinalIsoMdocRedirectSessionTranscript {
	@Override
	@PreEnvironment(strings = { "client_id", "nonce"}, required = "authorization_request_object" )
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {

		JsonObject jwkJson = null;
		String clientId = env.getString("client_id");
		String nonce =  env.getString("nonce");
		// this could be redirect_uri if response_uri isn't present, but we currently only support response_modes where response_uri is present
		String responseUri = env.getString("authorization_request_object", "claims.response_uri");

		calculateSessionTranscript(env, jwkJson, clientId, nonce, responseUri);

		return env;
	}

}
