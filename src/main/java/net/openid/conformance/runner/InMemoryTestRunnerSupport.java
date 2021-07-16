package net.openid.conformance.runner;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestModule.Status;

public class InMemoryTestRunnerSupport implements TestRunnerSupport {

	private Duration closedTestTimeout = Duration.ofMinutes(15);
	private Duration waitingTestTimeout = Duration.ofMinutes(360);

	@Autowired
	private AuthenticationFacade authenticationFacade;

	// collection of all currently running tests
	private Map<String, TestModule> runningTests = new LinkedHashMap<>();

	// collection of aliases assigned to tests
	private Map<String, String> aliases = new HashMap<>();

	/* (non-Javadoc)
	 * @see TestRunnerSupport#addRunningTest(java.lang.String, TestModule)
	 */
	@Override
	public synchronized void addRunningTest(String id, TestModule test) {
		runningTests.put(id, test);
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#hasAlias(java.lang.String)
	 */
	@Override
	public synchronized boolean hasAlias(String alias) {
		return aliases.containsKey(alias);
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#getRunningTestByAlias(java.lang.String)
	 */
	@Override
	public synchronized TestModule getRunningTestByAliasIgnoringLoggedInUser(String alias) {
		expireOldTests();
		String testId = getTestIdForAlias(alias);
		return runningTests.get(testId);
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#addAlias(java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized void addAlias(String alias, String id) {
		aliases.put(alias, id);
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#getRunningTestById(java.lang.String)
	 */
	@Override
	public synchronized TestModule getRunningTestById(String testId) {
		expireOldTests();

		if (authenticationFacade.getPrincipal() == null || 	// if the user's not logged in at all (it's a back-channel or Selenium call)
			authenticationFacade.isAdmin()) { 				// of if they're admin
			return runningTests.get(testId); 				// just send the results
		} else {
			TestModule test = runningTests.get(testId);		// otherwise make sure only the current user can get the test information
			if (test != null &&
				test.getOwner().equals(authenticationFacade.getPrincipal())) {
				return test;
			}
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#getAllRunningTestIds()
	 */
	@Override
	public synchronized Set<String> getAllRunningTestIds() {
		expireOldTests();

		if (authenticationFacade.getPrincipal() == null || 	// if the user's not logged in at all (it's a back-channel or Selenium call)
			authenticationFacade.isAdmin()) { 				// of if they're admin
			return runningTests.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().getCreated().compareTo(e1.getValue().getCreated())) // this sorts to newest-first
				.map(e -> e.getValue().getId())
				.collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			return runningTests.entrySet().stream()
				.filter(map -> map.getValue().getOwner().equals(owner))
				.sorted((e1, e2) -> e2.getValue().getCreated().compareTo(e1.getValue().getCreated())) // this sorts to newest-first
				.map(e -> e.getValue().getId())
				.collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
		}
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#getTestIdForAlias(java.lang.String)
	 */
	@Override
	public synchronized String getTestIdForAlias(String alias) {
		return aliases.get(alias);
	}

	/* (non-Javadoc)
	 * @see TestRunnerSupport#hasTestId(java.lang.String)
	 */
	@Override
	public synchronized boolean hasTestId(String testId) {
		expireOldTests();
		return runningTests.containsKey(testId);
	}

	/**
	 * @param testId
	 */
	@Override
	public synchronized void removeRunningTest(String testId) {
		runningTests.remove(testId);
	}

	private void expireOldTests() {
		for (Map.Entry<String, TestModule> entry : new HashSet<>(runningTests.entrySet())) {
			// if the test has been finished or interrupted, we check to see if it's timed out yet
			if ((entry.getValue().getStatus().equals(Status.FINISHED)
				|| entry.getValue().getStatus().equals(Status.INTERRUPTED))
					&& entry.getValue().getStatusUpdated().plus(getClosedTestTimeout()).isBefore(Instant.now())) {

				removeRunningTest(entry.getKey());

			}
			else if(entry.getValue().getStatus().equals(Status.WAITING)
					&& entry.getValue().getStatusUpdated().plus(waitingTestTimeout).isBefore(Instant.now())) {
				removeRunningTest(entry.getKey());
			}
		}
	}

	/**
	 * @return the closedTestTimeout
	 */
	public Duration getClosedTestTimeout() {
		return closedTestTimeout;
	}

	/**
	 * @param closedTestTimeout
	 *            the closedTestTimeout to set
	 */
	public void setClosedTestTimeout(Duration closedTestTimeout) {
		this.closedTestTimeout = closedTestTimeout;
	}

}
