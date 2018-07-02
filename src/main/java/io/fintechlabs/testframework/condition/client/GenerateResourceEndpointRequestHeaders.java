package io.fintechlabs.testframework.condition.client;

import java.util.Date;

import org.apache.http.client.utils.DateUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class GenerateResourceEndpointRequestHeaders extends AbstractCondition {

	public GenerateResourceEndpointRequestHeaders(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "resource" })
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		Date loginDate = new Date(); // User just logged in

		/*
		String institutionId = env.getString("resource", "institution_id");
		if (Strings.isNullOrEmpty(institutionId)) {
			throw error("Institution ID not found in resource configuration");
		}
		*/

		JsonObject headers = new JsonObject();

		headers.addProperty("x-fapi-auth-date", DateUtils.formatDate(loginDate));
		//headers.addProperty("x-fapi-financial-id", institutionId);

		env.put("resource_endpoint_request_headers", headers);

		logSuccess("Generated headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
