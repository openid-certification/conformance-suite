package net.openid.conformance.runner;

import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void callbacksRemovedAfterPublish() {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		AtomicReference<Status> first = new AtomicReference<>();
		svc.register("test-A", first::set);
		svc.publishStatusChange("test-A", Status.RUNNING);
		assertEquals(Status.RUNNING, first.get());

		// Subsequent publish must not re-invoke the now-cleared callback
		svc.publishStatusChange("test-A", Status.FINISHED);
		assertEquals(Status.RUNNING, first.get()); // unchanged
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
	void mapEntryRemovedAfterPublish() throws Exception {
		TestStatusWaiterService svc = new TestStatusWaiterService();
		svc.register("test-A", ignored -> {});
		svc.publishStatusChange("test-A", Status.FINISHED);

		Field f = TestStatusWaiterService.class.getDeclaredField("callbacksByTestId");
		f.setAccessible(true);
		Map<?, ?> map = (Map<?, ?>) f.get(svc);
		assertTrue(map.isEmpty(), "stale entry left after publish drained callbacks");
	}
}
