package io.fintechlabs.testframework.condition.rs;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

/**
 * @author jricher
 *
 */
public abstract class AbstractOpenBankingApiResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	protected AbstractOpenBankingApiResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected JsonObject createResponse(JsonObject data) {
		JsonObject response = new JsonObject();

		response.add("Data", data);

		JsonObject meta = new JsonObject();
		meta.addProperty("TotalPages", 1);
		response.add("Meta", meta);

		return response;
	}

}
