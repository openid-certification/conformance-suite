package net.openid.conformance.openinsurance.testmodule.Patrimonial.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.util.JSON;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class PolicyIDAllSelector extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "all_policies")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject patrimonialList = JsonParser.parseString(entityString).getAsJsonObject();

		JsonArray data = patrimonialList.getAsJsonArray("data");
		if(data.isEmpty()) {
			throw error("Data field is empty, no further processing required.");
		}

		JsonArray allPolicies = new JsonArray();
		for (JsonElement brand : data) {

			if (!brand.isJsonObject()) {
				throw error("Unexpected error: element of data object is not a real Json.");
			}

			JsonArray companies = brand.getAsJsonObject().getAsJsonArray("companies");
			for (JsonElement company : companies) {
				if (!company.isJsonObject()) {
					throw error("Unexpected error: element company is not a real Json.");
				}

				JsonArray policies = company.getAsJsonObject().getAsJsonArray("policies");
				allPolicies.addAll(policies);
			}
		}
		env.putString("all_policies", allPolicies.toString());


		logSuccess(String.format("All policies have been retrieved", allPolicies));
		return env;
	}

}
