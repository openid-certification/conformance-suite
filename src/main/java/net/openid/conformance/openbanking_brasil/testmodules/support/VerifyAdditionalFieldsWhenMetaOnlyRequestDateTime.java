package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full", strings = "metaOnlyRequestDateTime")
	public Environment evaluate(Environment env) {
		String metaOnlyRequestDateTime = env.getString("metaOnlyRequestDateTime");

		if(metaOnlyRequestDateTime.equals("true")) {

			JsonObject body = JsonParser.parseString(env.getString("resource_endpoint_response_full", "body"))
				.getAsJsonObject();

			if(JsonHelper.ifExists(body, "$.meta.totalRecords") || JsonHelper.ifExists(body, "$.meta.totalPages")) {
				 throw error ("In the MetaOnlyRequestDateTime object there should be no totalRecords or totalPages field");
			}

		} else {
			throw error ("VerifyAdditionalFieldsWhenMetaOnlyRequestDateTime has been used where metaOnlyRequestDateTime is false");
		}

		return env;
	}

}
