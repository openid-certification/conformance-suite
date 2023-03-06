package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AustraliaConnectIdAddClaimsSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		List<String> claimsToAdd = List.of(AustraliaConnectIdCheckClaimsSupported.ConnectIdMandatoryToSupportClaims);
		JsonArray claimsSupported = new JsonArray();

		for (String claim: claimsToAdd) {
			claimsSupported.add(claim);
		}

		server.add("claims_supported", claimsSupported);

		log("Added ConnectID required claims to claims_supported in server metadata", args("server", server));

		return env;
	}

}
