/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.runner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.fintechlabs.testframework.testmodule.TestModule;

/**
 * @author jricher
 *
 */
public class InMemoryTestRunnerSupport implements TestRunnerSupport {

	// collection of all currently running tests
	private Map<String, TestModule> runningTests = new HashMap<>();
	
	// collection of aliases assigned to tests
	private Map<String, String> aliases = new HashMap<>();
	

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#addRunningTest(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule)
	 */
	@Override
	public void addRunningTest(String id, TestModule test) {
		runningTests.put(id, test);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#hasAlias(java.lang.String)
	 */
	@Override
	public boolean hasAlias(String alias) {
		return aliases.containsKey(alias);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#getRunningTestByAlias(java.lang.String)
	 */
	@Override
	public TestModule getRunningTestByAlias(String alias) {
		return getRunningTestById(getTestIdForAlias(alias));
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#addAlias(java.lang.String, java.lang.String)
	 */
	@Override
	public void addAlias(String alias, String id) {
		aliases.put(alias, id);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#getRunningTestById(java.lang.String)
	 */
	@Override
	public TestModule getRunningTestById(String testId) {
		return runningTests.get(testId);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#getAllRunningTestIds()
	 */
	@Override
	public Set<String> getAllRunningTestIds() {
		return runningTests.keySet();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#getTestIdForAlias(java.lang.String)
	 */
	@Override
	public String getTestIdForAlias(String alias) {
		return aliases.get(alias);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#hasTestId(java.lang.String)
	 */
	@Override
	public boolean hasTestId(String testId) {
		return runningTests.containsKey(testId);
	}

}
