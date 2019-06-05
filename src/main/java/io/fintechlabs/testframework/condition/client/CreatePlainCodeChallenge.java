package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreatePlainCodeChallenge extends AbstractCondition{

	@Override
	@PreEnvironment(strings = "code_verifier")
	@PostEnvironment(strings = {"code_challenge","code_challenge_method"})
	public Environment evaluate(Environment env) {
		String verifier = env.getString("code_verifier");

		if(Strings.isNullOrEmpty(verifier)){
			throw error("code_verifier was null or empty");
		}

		env.putString("code_challenge", verifier);
		env.putString("code_challenge_method", "plain");
		log("Created code_challenge value", args("code_challenge", verifier));


		return env;
	}
}
