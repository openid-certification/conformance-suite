package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.RandomStringGenerator;

public class OIDCCCreateClientSecretForDynamicClient extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		String randomStr = RFC6749AppendixASyntaxUtils.generateVSChar(15, 5, 5);
		String secret = "secret_" + randomStr;
		client.addProperty("client_secret", secret);
		env.putObject("client", client);
		logSuccess("Set the secret for registered client", args("client_secret", secret));
		return env;
	}
}
