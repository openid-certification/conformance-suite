package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.IdmvpCheckClaimsSupported;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class IdmvpAddClaimsSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		List<String> claimsToAdd = List.of(IdmvpCheckClaimsSupported.idmvpMandatoryToSupportClaims);
		JsonArray claimsSupported = new JsonArray();

		for (String claim: claimsToAdd) {
			claimsSupported.add(claim);
		}

		server.add("claims_supported", claimsSupported);

		log("Added IDMVP required claims to claims_supported in server metadata", args("server", server));

		return env;
	}

}
