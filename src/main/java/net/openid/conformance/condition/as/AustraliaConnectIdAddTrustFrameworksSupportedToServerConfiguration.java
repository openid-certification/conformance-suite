package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckTrustFrameworkSupported;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdAddTrustFrameworksSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonArray frameworksSupported = new JsonArray();
		frameworksSupported.add(AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework);

		server.add("trust_frameworks_supported", frameworksSupported);

		log("Added ConnectID trust framework to trust_frameworks_supported in server metadata", args("server", server));

		return env;
	}

}
