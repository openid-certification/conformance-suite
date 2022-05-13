package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import springfox.documentation.spring.web.json.Json;

public class ValidateNumberOfRecordsPage1 extends ValidateNumberOfRecords{
	@Override
	public Environment evaluate(Environment env) {

		return executeNumberOfPageCheck(env, "first");
	}

}
