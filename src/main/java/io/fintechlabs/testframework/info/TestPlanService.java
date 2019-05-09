package io.fintechlabs.testframework.info;

import java.util.Map;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.pagination.PaginationRequest;

public interface TestPlanService {

	/**
	 * @param planId
	 * @param testName
	 * @param id
	 */
	void updateTestPlanWithModule(String planId, String testName, String id);

	void createTestPlan(String id, String planName, JsonObject config, String description, String[] testModules, String summary, String publish);

	/**
	 * @param id
	 * @return
	 */
	Map getTestPlan(String id);

	/**
	 * Returns the configuration to be used for a module of a given name
	 * @param planId
	 * @param moduleName
	 * @return
	 */
	JsonObject getModuleConfig(String planId, String moduleName);

	/**
	 * @return
	 */
	Map getPaginatedPlansForCurrentUser(PaginationRequest page);

	/**
	 *
	 */
	Map getPaginatedPublicPlans(PaginationRequest page);

	/**
	 * @param id
	 */
	Map getPublicPlan(String id);

	/**
	 * Sets published status of test plan and latest tests
	 * @param id Plan ID
	 * @param publish Publish status: null (unpublish), "summary" or "everything"
	 * @return true for success; false if not allowed
	 */
	boolean publishTestPlan(String id, String publish);

	void createIndexes();
}
