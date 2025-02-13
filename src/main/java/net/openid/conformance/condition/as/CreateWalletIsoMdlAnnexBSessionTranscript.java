package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateWalletIsoMdlAnnexBSessionTranscript extends AbstractMdocSessionTranscript {

	@Override
	@PreEnvironment(strings = "mdoc_generated_nonce")
	@PostEnvironment(strings = "session_transcript")
	public Environment evaluate(Environment env) {
		String mdocGeneratedNonce = env.getString("mdoc_generated_nonce");

		String clientId = env.getString("client", "client_id");
		String responseUri = env.getString("authorization_request_object", "claims.response_uri");
		String nonce = env.getString("nonce");

		createSessionTranscript(env, clientId, responseUri, nonce, mdocGeneratedNonce);

		return env;
	}

}
