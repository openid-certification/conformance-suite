package net.openid.conformance.info;

import com.google.gson.JsonObject;

import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.variant.VariantSelection;

public interface TestPlanService {

	/**
	 * @param planId
	 * @param testName
	 * @param id
	 */
	void updateTestPlanWithModule(String planId, String testName, String id);

	void createTestPlan(String id, String planName, VariantSelection variant, JsonObject config, String description, String[] testModules, String summary, String publish);

	/**
	 * @param id
	 * @return
	 */
	Plan getTestPlan(String id);

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
	PaginationResponse<Plan> getPaginatedPlansForCurrentUser(PaginationRequest page);

	/**
	 *
	 */
	PaginationResponse<PublicPlan> getPaginatedPublicPlans(PaginationRequest page);

	/**
	 * @param id
	 */
	PublicPlan getPublicPlan(String id);

	/**
	 * Sets published status of test plan and latest tests
	 * @param id Plan ID
	 * @param publish Publish status: null (unpublish), "summary" or "everything"
	 * @return true for success; false if not allowed
	 */
	boolean publishTestPlan(String id, String publish);

	/**
	 * Gets the test variant to apply for all tests in the the plan
	 * @param planId Plan ID
	 */
	VariantSelection getTestPlanVariant(String planId);

	void createIndexes();
}
