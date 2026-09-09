package net.openid.conformance.openid.ssf.eventstore;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore.EventsBatch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RFC 8936 2.5: with a long poll the transmitter "SHALL delay responding until a SET is
 * available or the timeout interval has elapsed" - so the wait ends as soon as something is
 * available, it does not run out the whole window.
 */
public class OIDSSFInMemoryEventStore_UnitTest {

	private static final String STREAM = "stream-1";

	private final OIDSSFInMemoryEventStore store = new OIDSSFInMemoryEventStore();

	private static OIDSSFSecurityEvent event(String jti) {
		return new OIDSSFSecurityEvent(jti, "set-" + jti, "urn:example:event");
	}

	@Test
	void shortPollReturnsImmediatelyWhenNothingIsQueued() {
		Instant start = Instant.now();
		EventsBatch batch = store.pollEvents(STREAM, 16, false, 10);
		assertTrue(batch.events().isEmpty());
		assertTrue(Duration.between(start, Instant.now()).toMillis() < 1000);
	}

	@Test
	void longPollReturnsAsSoonAsAnEventArrives() throws Exception {
		CompletableFuture<EventsBatch> poll = CompletableFuture.supplyAsync(() -> store.pollEvents(STREAM, 16, true, 10));
		Thread.sleep(300);
		Instant stored = Instant.now();
		store.storeEvent(STREAM, event("a"));

		EventsBatch batch = poll.get(5, TimeUnit.SECONDS);
		assertEquals(1, batch.events().size());
		assertEquals("a", batch.events().get(0).jti());
		assertFalse(batch.moreAvailable());
		assertTrue(Duration.between(stored, Instant.now()).toMillis() < 3000, "the long poll must not run out its 10 s window once an event is available");
	}

	@Test
	void longPollDrainsWhatIsAlreadyQueuedUpToMaxEvents() {
		store.storeEvent(STREAM, event("a"));
		store.storeEvent(STREAM, event("b"));
		store.storeEvent(STREAM, event("c"));

		Instant start = Instant.now();
		EventsBatch batch = store.pollEvents(STREAM, 2, true, 10);
		assertEquals(2, batch.events().size());
		assertTrue(batch.moreAvailable());
		assertTrue(Duration.between(start, Instant.now()).toMillis() < 1000);
	}

	@Test
	void longPollTimesOutEmptyWhenNothingArrives() {
		Instant start = Instant.now();
		EventsBatch batch = store.pollEvents(STREAM, 16, true, 1);
		assertTrue(batch.events().isEmpty());
		assertTrue(Duration.between(start, Instant.now()).toMillis() >= 1000);
	}
}
