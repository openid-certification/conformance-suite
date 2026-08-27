package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrAddClaimsSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonArray claimsSupported = new JsonArray();
		claimsSupported.add("sub");
		claimsSupported.add("acr");
		claimsSupported.add("sharing_duration");
		server.add("claims_supported", claimsSupported);

		log("Added the claims CDR requires Data Holders to support to claims_supported in server metadata", args("server", server));

		return env;
	}

}
