package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateVP1FinalWalletIsoMdocRedirectSessionTranscript extends AbstractCreateVP1FinalIsoMdocRedirectSessionTranscript {
	@Override
	@PreEnvironment(strings = { "client_id", "nonce", "response_uri"} )
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {
		JsonObject jwkJson = env.getObject("decryption_jwk");
		String clientId = env.getString("client_id");
		String nonce =  env.getString("nonce");
		// this could be redirect_uri if response_uri isn't present, but we currently only support response_modes where response_uri is present
		String responseUri = env.getString("response_uri");

		calculateSessionTranscript(env, jwkJson, clientId, nonce, responseUri);

		return env;
	}

}
