package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSubjectTypesSupportedPairwiseToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonArray claimsSupported = new JsonArray();

		claimsSupported.add("pairwise");

		server.add("subject_types_supported", claimsSupported);

		log("Added pairwise to subject_types_supported in server metadata", args("server", server));

		return env;
	}

}
