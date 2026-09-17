package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TestInfoService {

	/**
	 * @param variant All variants for this test module
	 * @param variantFromPlanDefinition Any variants specified for this specific module in the plan definition
	 */
	void createTest(String id, String testName, VariantSelection variant, VariantSelection variantFromPlanDefinition, String url, JsonObject config, String alias, Instant started, String testPlanId, String description, String summary, String publish);

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

	/**
	 * Sets published status of test
	 * @param id Test ID
	 * @param publish Publish status: null (unpublish), "summary" or "everything"
	 * @return true for success; false if not allowed
	 */
	boolean publishTest(String id, String publish);

	/**
	 * One page of the test-log listing: every test for an admin, the caller's own for anyone
	 * else, narrowed by the filter and by the page's search term, ordered and paged as the page
	 * asks. Each row carries the name of the plan it belongs to.
	 *
	 * @param page   the page, ordering and search term asked for
	 * @param filter the statuses and results to narrow to
	 * @return that page, in the paging envelope the listing endpoints answer with
	 */
	PaginationResponse<TestInfo> getPaginatedTestsForCurrentUser(PaginationRequest page, TestListFilter filter);

	/**
	 * The same for the published tests, read through the public projection.
	 *
	 * @param page   the page, ordering and search term asked for
	 * @param filter the statuses and results to narrow to
	 * @return that page, in the paging envelope the listing endpoints answer with
	 */
	PaginationResponse<PublicTestInfo> getPaginatedPublicTests(PaginationRequest page, TestListFilter filter);

	void createIndexes();

	boolean deleteTests(List<String> id);

	/**
	 * Delete these tests and their log entries, scoped to an owner or to nobody in particular.
	 *
	 * <p>The scoping is a parameter rather than read from the security context, because the bulk
	 * delete runs on a background thread that has none - and because everything that makes a
	 * delete correct (matching TEST_INFO on the indexed _id and EVENT_LOG on testId, dropping
	 * the cached owner afterwards) has to happen there too, so there is one definition of it.
	 *
	 * @param ids   the test ids to delete
	 * @param owner whose tests may be deleted, or null for anyone's - which only a caller that
	 *              has already established the right to do so may pass
	 * @return how much went, so a bulk delete can report progress
	 */
	Deleted deleteTests(List<String> ids, Map<String, String> owner);

	/**
	 * @param tests        TEST_INFO documents removed
	 * @param logEntries   EVENT_LOG documents removed
	 * @param acknowledged whether the database acknowledged both removes
	 */
	record Deleted(long tests, long logEntries, boolean acknowledged) { }
}
