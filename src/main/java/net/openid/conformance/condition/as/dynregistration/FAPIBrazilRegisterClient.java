package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilRegisterClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "config"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("dynamic_registration_request");
		client.remove("software_statement");
		String randomStr = RFC6749AppendixASyntaxUtils.generateVSChar(20, 5, 0);
		client.addProperty("client_id", "client_" + randomStr);
		//Copy certificate from configuration
		client.addProperty("certificate", env.getString("config", "client.certificate"));
		env.putObject("client", client);
		logSuccess("Registered client", args("client", client));
		return env;
	}
}
