package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractUserInfoEndpointAsResource extends AbstractCondition {

	public ExtractUserInfoEndpointAsResource(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("server","userinfo_endpoint");
		if(Strings.isNullOrEmpty(resourceUrl)){
			throw error("Usering Endpoint not found");
		}
		JsonObject resource = new JsonObject();
		resource.addProperty("resourceUrl", resourceUrl);
		env.putObject("userinfo_resource", resource);
		return env;
	}
}
