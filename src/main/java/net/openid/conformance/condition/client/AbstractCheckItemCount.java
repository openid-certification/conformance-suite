package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.JsonHelper;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckItemCount extends AbstractJsonAssertingCondition {

	public Environment checkItemCount(Environment env, Integer minItems) {

		JsonElement apiResponse = bodyFrom(env);

		if (!JsonHelper.ifExists(apiResponse, "$.data")) {
			throw error("The response does not contain a data element.");
		}

		JsonElement dataElement = findByPath(apiResponse, "$.data");
        int totalRecords = 1;
		if (dataElement.isJsonArray()) {
            totalRecords = dataElement.getAsJsonArray().size();
        }

		if (totalRecords < minItems) {
			throw error("The response does not contain the required minimum number of data elements.");
		}

		return env;
	}
}
