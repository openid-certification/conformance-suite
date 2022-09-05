package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class OIDCCRegisterClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("dynamic_registration_request");
		String randomStr = RFC6749AppendixASyntaxUtils.generateVSChar(15, 5, 5);
		client.addProperty("client_id", "client_" + randomStr);
		env.putObject("client", client);
		logSuccess("Registered client", args("client", client));
		return env;
	}
}
