package net.openid.conformance.runner;

import java.util.Set;

import net.openid.conformance.testmodule.TestModule;

public interface TestRunnerSupport {

	/**
	 * @param id
	 * @param test
	 */
	void addRunningTest(String id, TestModule test);

	/**
	 * @param alias
	 * @return
	 */
	boolean hasAlias(String alias);

	/**
	 * @param alias
	 * @return
	 */
	TestModule getRunningTestByAlias(String alias);

	/**
	 * @param alias
	 * @param id
	 */
	void addAlias(String alias, String id);

	/**
	 * @param testId
	 * @return
	 */
	TestModule getRunningTestById(String testId);

	/**
	 * @return
	 */
	Set<String> getAllRunningTestIds();

	/**
	 * @param alias
	 * @return
	 */
	String getTestIdForAlias(String alias);

	/**
	 * @param testId
	 * @return
	 */
	boolean hasTestId(String testId);

	/**
	 * @param testId
	 */
	void removeRunningTest(String testId);

}
