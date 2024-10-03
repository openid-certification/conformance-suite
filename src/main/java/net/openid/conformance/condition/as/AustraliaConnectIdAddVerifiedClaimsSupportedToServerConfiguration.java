package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdAddVerifiedClaimsSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		server.addProperty("verified_claims_supported", true);

		log("Added ConnectID verified_claims_supported to server metadata", args("server", server));

		return env;
	}
}
