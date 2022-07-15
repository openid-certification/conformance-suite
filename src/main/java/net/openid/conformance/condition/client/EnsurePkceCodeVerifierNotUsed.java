package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsurePkceCodeVerifierNotUsed extends AbstractCondition {

	public static final String ENV_KEY = "code_verifier_list";

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		String codeVerifier = env.getString("token_endpoint_request", "body_form_params.code_verifier");
		String usedCodeVerifier = env.getString(ENV_KEY, codeVerifier);
		if(Strings.isNullOrEmpty(usedCodeVerifier)) {
			logSuccess("Code verifier has not been used", args("code_verifier", codeVerifier));
			env.putString(ENV_KEY, codeVerifier, "1");
		} else {
			throw error("code verifier has been used", args("code verifier", codeVerifier));
		}
		return env;
	}

}
