package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilOPINRegisterClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "config"}, strings = "registration_access_token")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("dynamic_registration_request").deepCopy();
		String randomStr = RFC6749AppendixASyntaxUtils.generateVSChar(20, 5, 0);
		client.addProperty("client_id", "client_" + randomStr);
		//Copy certificate from configuration
		client.addProperty("certificate", env.getString("config", "client.certificate"));
		client.addProperty("registration_access_token", env.getString("registration_access_token"));
		String registrationClientUri = env.getString("registration_client_uri", "fullUrl");
		client.addProperty("registration_client_uri", registrationClientUri);

		if (!client.has("scope")) {
			// we don't know what scope the client will want, but the OB Brazil DCR spec requires us to fill in scope.
			// we could check the regulator role in the SSA to see what scopes the client is entitled to, but that seems
			// like overkill.
			client.addProperty("scope", "openid customers consents");
		}
		env.putObject("client", client);
		logSuccess("Registered client", args("client", client));
		return env;
	}
}
