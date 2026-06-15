package net.openid.conformance.runner;

import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestStatusWaiterService_UnitTest {

	@Test
	void publishInvokesRegisteredCallback() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		AtomicReference<Status> received = new AtomicReference<>();
		svc.register("test-A", received::set);

		svc.publishStatusChange("test-A", Status.FINISHED);

		assertEquals(Status.FINISHED, received.get());
	}

	@Test
	void publishWithNoCallbacksIsNoOp() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		svc.publishStatusChange("test-A", Status.FINISHED); // must not throw
	}

	@Test
	void publishInvokesAllCallbacksForSameTest() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		AtomicReference<Status> a = new AtomicReference<>();
		AtomicReference<Status> b = new AtomicReference<>();
		svc.register("test-A", a::set);
		svc.register("test-A", b::set);

		svc.publishStatusChange("test-A", Status.FINISHED);

		assertEquals(Status.FINISHED, a.get());
		assertEquals(Status.FINISHED, b.get());
	}

	@Test
	void publishDoesNotInvokeCallbacksForOtherTest() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		AtomicReference<Status> received = new AtomicReference<>();
		svc.register("test-A", received::set);

		svc.publishStatusChange("test-B", Status.FINISHED);

		assertNull(received.get());
	}

	@Test
	void callbacksPersistAcrossPublishes() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		AtomicReference<Status> last = new AtomicReference<>();
		svc.register("test-A", last::set);
		svc.publishStatusChange("test-A", Status.RUNNING);
		assertEquals(Status.RUNNING, last.get());

		// A waiter may be interested in a later state, not merely the next one, so the
		// callback stays registered and a subsequent publish invokes it again. Cleanup
		// is the caller's job via unregister, not a side-effect of publish.
		svc.publishStatusChange("test-A", Status.FINISHED);
		assertEquals(Status.FINISHED, last.get());
	}

	@Test
	void unregisterPreventsCallback() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		Consumer<Status> cb = s -> fail("should not be called after unregister");
		svc.register("test-A", cb);
		svc.unregister("test-A", cb);

		svc.publishStatusChange("test-A", Status.FINISHED);
	}

	@Test
	void mapEntryRemovedAfterUnregisterOfSoleCallback() throws Exception {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		Consumer<Status> cb = ignored -> {};
		svc.register("test-A", cb);
		svc.unregister("test-A", cb);

		Field f = TestStatusWaiterService.class.getDeclaredField("callbacksByTestId");
		f.setAccessible(true);
		Map<?, ?> map = (Map<?, ?>) f.get(svc);
		assertTrue(map.isEmpty(), "stale empty entry left in map after last unregister");
	}

	@Test
	void mapEntryRetainedAfterPublishUntilUnregister() throws Exception {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		Consumer<Status> cb = ignored -> {};
		svc.register("test-A", cb);
		svc.publishStatusChange("test-A", Status.FINISHED);

		Field f = TestStatusWaiterService.class.getDeclaredField("callbacksByTestId");
		f.setAccessible(true);
		Map<?, ?> map = (Map<?, ?>) f.get(svc);
		// Publish no longer drains callbacks — they persist for future transitions.
		assertFalse(map.isEmpty(), "publish must not drop callbacks; cleanup is via unregister");

		svc.unregister("test-A", cb);
		assertTrue(map.isEmpty(), "entry should be gone after the sole callback unregisters");
	}
}
