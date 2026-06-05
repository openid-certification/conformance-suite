package net.openid.conformance.runner;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestModule;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InMemoryTestRunnerSupport implements TestRunnerSupport {

	private Duration closedTestTimeout = Duration.ofMinutes(15);
	private Duration waitingTestTimeout = Duration.ofHours(6);

	// expireOldTests() walks the running-test map and allocates a HashSet
	// every call; it used to fire on every lookup, showing up as ~7% of
	// Spring-handler CPU in JFR profiles. Rate-limit it: tests time out
	// on the order of 15 min / 6 h, so cleaning up a minute late is fine.
	private static final Duration EXPIRE_INTERVAL = Duration.ofSeconds(60);
	private Instant lastExpireRun = Instant.EPOCH;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	// collection of all currently running tests
	private Map<String, TestModule> runningTests = new LinkedHashMap<>();

	// collection of aliases assigned to tests
	private Map<String, String> aliases = new HashMap<>();

	@Override
	public synchronized void addRunningTest(String id, TestModule test) {
		runningTests.put(id, test);
	}

	@Override
	public synchronized boolean hasAlias(String alias) {
		return aliases.containsKey(alias);
	}

	@Override
	public synchronized TestModule getRunningTestByAliasIgnoringLoggedInUser(String alias) {
		expireOldTests();
		String testId = getTestIdForAlias(alias);
		return runningTests.get(testId);
	}

	@Override
	public synchronized void addAlias(String alias, String id) {
		aliases.put(alias, id);
	}

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

	@Override
	public synchronized String getTestIdForAlias(String alias) {
		return aliases.get(alias);
	}

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
		Instant now = Instant.now();
		if (lastExpireRun.plus(EXPIRE_INTERVAL).isAfter(now)) {
			return;
		}
		lastExpireRun = now;
		for (Map.Entry<String, TestModule> entry : new HashSet<>(runningTests.entrySet())) {
			TestModule testModule = entry.getValue();
			String testId = entry.getKey();
			switch (testModule.getStatus()) {
				case INTERRUPTED:
				case FINISHED:
					// if the test has been finished or interrupted, we check to see if it's timed out yet
					if (testModule.getStatusUpdated().plus(getClosedTestTimeout()).isBefore(Instant.now())) {
						removeRunningTest(testId);
					}
					break;

				case RUNNING:
					// A RUNNING test is holding the test lock (setStatusInternal keeps the lock for RUNNING),
					// so stop() would block forever trying to acquire it and can never actually stop the
					// test. Removing it from the list would just hide a test that is still holding a thread
					// and the lock (and the stop() we used to fire here leaked another thread blocked on the
					// lock). Leave it in the list so it stays visible; it is cleaned up normally once it
					// reaches a terminal state, or on process restart.
					// See https://gitlab.com/openid/conformance-suite/-/work_items/1827
					break;

				case CREATED:
				case WAITING:
				case CONFIGURED:
				case NOT_YET_CREATED:
					// these states have released the lock, so stop() can actually take effect
					if (testModule.getStatusUpdated().plus(waitingTestTimeout).isBefore(Instant.now())) {
						removeRunningTest(testId);
						testModule.getTestExecutionManager().runInBackground(() -> {
							testModule.stop("The test was idle for more than %s minutes.".formatted(waitingTestTimeout.toMinutes()));
							return "stopped";
						});
					}
					break;
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
