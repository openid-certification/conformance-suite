package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetUserinfoSignedResponseAlgToRS256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		client.addProperty("userinfo_signed_response_alg", "RS256");
		log("Set userinfo_signed_response_alg to RS256");
		env.putObject("client", client);
		return env;
	}
}
