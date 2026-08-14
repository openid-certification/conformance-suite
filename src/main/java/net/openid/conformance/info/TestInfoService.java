package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;

import java.time.Instant;
import java.util.List;

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

	void createIndexes();

	boolean deleteTests(List<String> id);

	/**
	 * Hand every test owned by the given legacy (issuer, subject) over to the
	 * currently authenticated user, along with the log entries and screenshots
	 * belonging to those tests.
	 *
	 * @return how many documents changed hands in each collection
	 */
	MigrationCounts migrateOwnership(String oldIss, String oldSub);

	/**
	 * What a single ownership migration moved.
	 *
	 * @param tests      documents re-owned in TEST_INFO
	 * @param logEntries documents re-owned in EVENT_LOG — the test log, including
	 *                   the entries that carry uploaded screenshots
	 */
	record MigrationCounts(long tests, long logEntries) {

		public static final MigrationCounts NONE = new MigrationCounts(0, 0);

		public boolean movedNothing() {
			return tests == 0 && logEntries == 0;
		}
	}
}
