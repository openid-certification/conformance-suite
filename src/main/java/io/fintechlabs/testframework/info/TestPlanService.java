package io.fintechlabs.testframework.info;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

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
	List<Map> getAllPlansForCurrentUser();

	/**
	 *
	 */
	List<Map> getPublicPlans();

}
