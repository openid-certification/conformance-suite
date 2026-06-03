package net.openid.conformance.runner;

import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers expireOldTests(): a RUNNING test holds the lock so stop() can't work on it - it must be left
 * in the running list rather than removed (which would hide it) or stopped (which would block a thread
 * forever). Lock-released states are still expired. See
 * https://gitlab.com/openid/conformance-suite/-/work_items/1827
 */
@ExtendWith(MockitoExtension.class)
public class InMemoryTestRunnerSupport_UnitTest {

	private InMemoryTestRunnerSupport support;

	@Mock
	private AuthenticationFacade authenticationFacade;

	@BeforeEach
	public void setup() {
		support = new InMemoryTestRunnerSupport();
		ReflectionTestUtils.setField(support, "authenticationFacade", authenticationFacade);
		// not-logged-in / back-channel path returns all running tests
		lenient().when(authenticationFacade.getPrincipal()).thenReturn(null);
	}

	private TestModule mockTest(String id, TestModule.Status status, Instant statusUpdated) {
		TestModule t = mock(TestModule.class);
		lenient().when(t.getId()).thenReturn(id);
		lenient().when(t.getStatus()).thenReturn(status);
		lenient().when(t.getStatusUpdated()).thenReturn(statusUpdated);
		lenient().when(t.getCreated()).thenReturn(statusUpdated);
		return t;
	}

	@Test
	public void runningTestPastTimeout_isLeftInListAndNotStopped() {
		// older than waitingTestTimeout (6h)
		TestModule running = mockTest("RUNNINGTEST0001", TestModule.Status.RUNNING,
			Instant.now().minus(7, ChronoUnit.HOURS));
		support.addRunningTest("RUNNINGTEST0001", running);

		assertThat(support.getAllRunningTestIds()).contains("RUNNINGTEST0001");
		verify(running, never()).stop(anyString());
		verify(running, never()).getTestExecutionManager();
	}

	@Test
	public void waitingTestPastTimeout_isRemovedAndStopScheduled() {
		TestModule waiting = mockTest("WAITINGTEST0001", TestModule.Status.WAITING,
			Instant.now().minus(7, ChronoUnit.HOURS));
		TestExecutionManager tem = mock(TestExecutionManager.class);
		when(waiting.getTestExecutionManager()).thenReturn(tem);
		support.addRunningTest("WAITINGTEST0001", waiting);

		assertThat(support.getAllRunningTestIds()).doesNotContain("WAITINGTEST0001");
		verify(tem).runInBackground(any());
	}
}
