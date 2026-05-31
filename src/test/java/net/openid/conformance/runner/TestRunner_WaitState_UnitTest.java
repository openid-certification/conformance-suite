package net.openid.conformance.runner;

import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TestRunner_WaitState_UnitTest {

	private TestRunner runner;
	private TestRunnerSupport support;
	private TestStatusWaiterService waiterService;

	@BeforeEach
	void setUp() {
		runner = new TestRunner();
		support = Mockito.mock(TestRunnerSupport.class);
		waiterService = new TestStatusWaiterService();
		// The fields are @Autowired; for a unit test we set them directly.
		ReflectionTestUtils.setField(runner, "support", support);
		ReflectionTestUtils.setField(runner, "testStatusWaiterService", waiterService);
	}

	@Test
	void unknownOrUnauthorizedTestReturns404WithJsonMarker() {
		when(support.getRunningTestById(eq("missing"))).thenReturn(null);

		DeferredResult<ResponseEntity<Map<String, Object>>> result =
			runner.waitForState("missing", "RUNNING", 5000L);

		assertNotNull(result.getResult(), "should resolve immediately for unknown test");
		ResponseEntity<?> resp = (ResponseEntity<?>) result.getResult();
		assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
		// The JSON marker is what lets the python capability probe
		// distinguish "endpoint deployed but test missing" from
		// "endpoint not on this build at all" (which would be Spring's
		// generic 404). If you change the body shape, update the probe
		// in scripts/conformance.py._probe_wait_state_supported.
		assertEquals(MediaType.APPLICATION_JSON, resp.getHeaders().getContentType());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) resp.getBody();
		assertEquals("test not found", body.get("error"));
	}

	@Test
	void timeoutClampedAtUpperBound() {
		assertEquals(30_000L, TestRunner.clampWaitStateTimeoutMs(30_000L));
		assertEquals(30_000L, TestRunner.clampWaitStateTimeoutMs(30_001L));
		assertEquals(30_000L, TestRunner.clampWaitStateTimeoutMs(9_999_999L));
		assertEquals(30_000L, TestRunner.clampWaitStateTimeoutMs(Long.MAX_VALUE));
	}

	@Test
	void timeoutClampedAtLowerBound() {
		assertEquals(1L, TestRunner.clampWaitStateTimeoutMs(1L));
		assertEquals(1L, TestRunner.clampWaitStateTimeoutMs(0L));
		assertEquals(1L, TestRunner.clampWaitStateTimeoutMs(-1L));
		assertEquals(1L, TestRunner.clampWaitStateTimeoutMs(Long.MIN_VALUE));
	}

	@Test
	void timeoutNotChangedWhenInRange() {
		assertEquals(15_000L, TestRunner.clampWaitStateTimeoutMs(15_000L));
		assertEquals(100L, TestRunner.clampWaitStateTimeoutMs(100L));
	}

	@Test
	void fastPathReturnsImmediatelyWhenStateDiffersFromCurrentState() {
		TestModule test = Mockito.mock(TestModule.class);
		when(test.getStatus()).thenReturn(Status.FINISHED);
		when(support.getRunningTestById(eq("test-A"))).thenReturn(test);

		DeferredResult<ResponseEntity<Map<String, Object>>> result =
			runner.waitForState("test-A", "RUNNING", 5000L);

		assertNotNull(result.getResult());
		ResponseEntity<?> resp = (ResponseEntity<?>) result.getResult();
		assertEquals(HttpStatus.OK, resp.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) resp.getBody();
		assertEquals("FINISHED", body.get("state"));
	}

	@Test
	void longPollResolvesWhenStatusPublished() throws Exception {
		TestModule test = Mockito.mock(TestModule.class);
		when(test.getStatus()).thenReturn(Status.RUNNING);
		when(support.getRunningTestById(eq("test-A"))).thenReturn(test);

		DeferredResult<ResponseEntity<Map<String, Object>>> result =
			runner.waitForState("test-A", "RUNNING", 5000L);

		assertNull(result.getResult(), "should be suspended while status matches currentState");

		waiterService.publishStatusChange("test-A", Status.FINISHED);

		// Setting the deferred is synchronous; result should be populated immediately.
		assertNotNull(result.getResult());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) ((ResponseEntity<?>) result.getResult()).getBody();
		assertEquals("FINISHED", body.get("state"));
	}

	@Test
	void registerThenRecheckCatchesRaceWithIntermediateStatusChange() {
		// Race window we're closing:
		//   t0  controller calls getRunningTestById(id)
		//   t1  setStatusInternal() publishes the new status (drains
		//       the empty callbacks queue — nothing happens)
		//   t2  controller calls register()
		//   t3  controller re-reads getStatus()
		//
		// If we only registered without the re-read, the controller would
		// suspend forever — the publish at t1 has already happened with
		// no callback to fire. The post-register re-read at t3 must see
		// the new status the publish at t1 committed.
		//
		// We simulate the publish-without-callback by swapping in a
		// waiterService whose register() flips the underlying status
		// BEFORE returning. The controller's first getStatus() reads
		// RUNNING; the post-register re-read reads FINISHED. If the
		// re-read is missing or broken, the deferred stays unresolved.
		TestModule test = Mockito.mock(TestModule.class);
		AtomicReference<Status> currentStatus = new AtomicReference<>(Status.RUNNING);
		when(test.getStatus()).thenAnswer(inv -> currentStatus.get());
		when(support.getRunningTestById(eq("test-A"))).thenReturn(test);

		TestStatusWaiterService racingService = new TestStatusWaiterService() {
			@Override
			public void register(String testId, Consumer<Status> callback) {
				super.register(testId, callback);
				// The transition happened in the race window. Note: we do
				// NOT call publishStatusChange — that publish corresponds
				// to t1 above, before this register() was made, so it had
				// no callback to fire and is lost from the waiter's POV.
				currentStatus.set(Status.FINISHED);
			}
		};
		ReflectionTestUtils.setField(runner, "testStatusWaiterService", racingService);

		DeferredResult<ResponseEntity<Map<String, Object>>> result =
			runner.waitForState("test-A", "RUNNING", 5000L);

		// The deferred MUST be resolved by the post-register re-read.
		// If this fails, the controller is relying on the (lost) callback
		// and the race window is open.
		assertNotNull(result.getResult(),
			"register-then-recheck must close the race; otherwise the deferred "
			+ "would suspend forever waiting for a callback that won't fire");
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) ((ResponseEntity<?>) result.getResult()).getBody();
		assertEquals("FINISHED", body.get("state"));
	}

	// Note: the "publish AFTER register" path (callback-fires resolution)
	// is already covered by `longPollResolvesWhenStatusPublished` above —
	// no need to duplicate it here.
}
