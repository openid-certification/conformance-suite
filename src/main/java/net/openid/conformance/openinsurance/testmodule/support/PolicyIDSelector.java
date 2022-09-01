package net.openid.conformance.openinsurance.testmodule.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class PolicyIDSelector extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "policyId")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject patrimonialList = JsonParser.parseString(entityString).getAsJsonObject();

		JsonArray data = patrimonialList.getAsJsonArray("data");
		if(data.isEmpty()) {
			throw error("Data field is empty, no further processing required.");
		}

		JsonObject firstPatrimonial = data.get(0).getAsJsonObject();

		JsonArray companies = firstPatrimonial.getAsJsonArray("companies");
		if(companies.isEmpty()) {
			throw error("Company field is empty, no further processing required.");
		}

		JsonObject firstCompany = companies.get(0).getAsJsonObject();

		JsonArray policies = firstCompany.getAsJsonArray("policies");
		if(policies.isEmpty()) {
			throw error("Policy field is empty, no further processing required.");
		}

		String policyId = OIDFJSON.getString(policies.get(0));

		env.putString("policyId", policyId);
		logSuccess(String.format("Specific policyID selected", policyId));
		return env;
	}

}
