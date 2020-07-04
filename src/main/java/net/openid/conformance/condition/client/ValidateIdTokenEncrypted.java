package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenEncrypted extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		JsonElement jweHeader = env.getElementFromObject("id_token", "jwe_header");

		if (jweHeader == null) {
			throw error("The id_token was not encrypted.");
		}

		logSuccess("id_token was encrypted");
		return env;

	}

}
