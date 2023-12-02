package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsurePkceCodeVerifierNotUsed extends AbstractCondition {

	private static final int CACHE_SIZE = 256;
	private static final List<String> cachedCodes = new ArrayList<>(CACHE_SIZE) ;

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {
		String codeVerifier = env.getString("token_endpoint_request", "body_form_params.code_verifier");
		if(Strings.isNullOrEmpty(codeVerifier)) {
			throw error("Code Verifier not found in request");
		}
		synchronized (cachedCodes) {
			if(cachedCodes.contains(codeVerifier)) {
				throw error("code verifier has been used", args("code verifier", codeVerifier));
			} else {
				if(cachedCodes.size() >= CACHE_SIZE) {
					cachedCodes.subList(0, 50).clear();
				}
				cachedCodes.add(codeVerifier);
				logSuccess("Code verifier has not been used", args("code_verifier", codeVerifier));
			}
		}
		return env;
	}

}
