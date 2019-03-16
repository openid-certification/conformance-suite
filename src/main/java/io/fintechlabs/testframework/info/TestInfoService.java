package io.fintechlabs.testframework.info;

import java.time.Instant;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.testmodule.TestModule.Result;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

public interface TestInfoService {

	void createTest(String id, String testName, String url, JsonObject config, String alias, Instant started, String testPlanId, String Description, String summary, String publish);

	/**
	 * Update the result of a test in the database
	 *
	 * @param id
	 * @param result
	 */
	void updateTestResult(String id, Result result);

	/**
	 * Update the status of a test in the database
	 *
	 * @param id
	 * @param status
	 */
	void updateTestStatus(String id, Status status);

	/**
	 * Get the owner of a test ID.
	 *
	 * @param id
	 * @return
	 */
	ImmutableMap<String, String> getTestOwner(String id);
}
