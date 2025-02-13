package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractMdocSessionTranscript;
import net.openid.conformance.testmodule.Environment;

public class CreateVerifierIsoMdlAnnexBSessionTranscript extends AbstractMdocSessionTranscript {
	@Override
	@PreEnvironment(strings = { "mdoc_generated_nonce", "response_uri", "nonce" }, required = "config")
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {
		String clientId = env.getString("config", "client.client_id");
		String responseUri = env.getString("response_uri");
		String nonce =  env.getString("nonce");
		String mdocGeneratedNonce = env.getString("mdoc_generated_nonce");
		createSessionTranscript(env, clientId, responseUri, nonce, mdocGeneratedNonce);

		return env;
	}

}
