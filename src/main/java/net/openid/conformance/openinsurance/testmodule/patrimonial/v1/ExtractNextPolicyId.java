package net.openid.conformance.openinsurance.testmodule.patrimonial.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractNextPolicyId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "all_policies")
	public Environment evaluate(Environment env) {

		String policiesString = env.getString("all_policies");
		JsonArray policies = JsonParser.parseString(policiesString).getAsJsonArray();

		if ()
		String policeString = policies.remove(0).getAsString();

		logSuccess(String.format("PolicyID extracted %s", policeString));

		return env;
	}

}
