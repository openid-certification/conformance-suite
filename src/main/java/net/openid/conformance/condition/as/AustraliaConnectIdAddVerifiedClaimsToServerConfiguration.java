package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckVerifiedClaimsSupported;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdAddVerifiedClaimsToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonArray claimsSupported = new JsonArray();

		for (String claim: AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims) {
			claimsSupported.add(claim);
		}

		server.add("claims_in_verified_claims_supported", claimsSupported);

		log("Added ConnectID valid verified claims to claims_in_verified_claims_supported in server metadata", args("server", server));

		return env;
	}

}
