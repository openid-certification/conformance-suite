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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule;

/**
 * @author jricher
 *
 */
public class InMemoryTestRunnerSupport implements TestRunnerSupport {

	@Autowired
	private AuthenticationFacade authenticationFacade;

	// collection of all currently running tests
	private Map<String, TestModule> runningTests = new LinkedHashMap<>();
	
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
		// Put in null check to handle non-userfacing interactions.
		if (authenticationFacade.getAuthenticationToken() == null ||
				authenticationFacade.isAdmin()) {
			return runningTests.get(testId);
		} else {
			TestModule test = runningTests.get(testId);
			if (test != null &&
					test.getOwner().equals((ImmutableMap<String,String>)authenticationFacade.getAuthenticationToken().getPrincipal())) {
				return test;
			}
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.runner.TestRunnerSupport#getAllRunningTestIds()
	 */
	@Override
	public Set<String> getAllRunningTestIds() {
		// Put in null check to handle non-userfacing interactions.
		if (authenticationFacade.getAuthenticationToken() == null ||
				authenticationFacade.isAdmin()) {
			return runningTests.keySet();
		} else {
			ImmutableMap<String,String> owner = (ImmutableMap<String,String>)authenticationFacade.getAuthenticationToken().getPrincipal();
			return runningTests.entrySet().stream()
					.filter(map -> map.getValue().getOwner().equals(owner))
					.sorted((e1, e2) -> e2.getValue().getCreated().compareTo(e1.getValue().getCreated())) // this sorts to newest-first
					.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();
		}
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
