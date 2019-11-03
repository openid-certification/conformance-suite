package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class OIDCCSetDynamicClientPassword extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		String secret = "secret_" + RandomStringUtils.randomAlphanumeric(10);
		client.addProperty("client_secret", secret);
		//TODO add other properties?
		env.putObject("client", client);
		logSuccess("Set password for registered client", args("client_secret", secret));
		return env;
	}
}

