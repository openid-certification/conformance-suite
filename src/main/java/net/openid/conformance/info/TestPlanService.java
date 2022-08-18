package net.openid.conformance.info;

import com.google.gson.JsonObject;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

public interface TestPlanService {

	/**
	 * @param variant Variant overrides for this test module, i.e. excluding those stored in the plan object
	 */
	void updateTestPlanWithModule(String planId, String testName, VariantSelection variant, String id);

	void createTestPlan(String id, String planName, VariantSelection variant, JsonObject config, String description, String certificationProfileName, List<Plan.Module> testModules, String summary, String publish);

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
	 * Changes plan immutable status
	 * @param id Plan ID
	 * @param immutable
	 * @return true for success; false if not allowed
	 */
	boolean changeTestPlanImmutableStatus(String id, Boolean immutable);

	/**
	 * Gets the test variant to apply for all tests in the the plan
	 * @param planId Plan ID
	 */
	VariantSelection getTestPlanVariant(String planId);

	void createIndexes();

	/**
	 * Delete a mutable plan
	 * @param id Plan ID
	 */
	void deleteMutableTestPlan(String id);
}
