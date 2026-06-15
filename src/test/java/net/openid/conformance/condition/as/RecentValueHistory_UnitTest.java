package net.openid.conformance.condition.as;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecentValueHistory_UnitTest {

	private static final String NS = "test-ns";

	@BeforeEach
	void clear() {
		// clear() also resets any retention override so tests don't pollute each other.
		RecentValueHistory.clear();
	}

	@Test
	void unseenValueReturnsNull() {
		assertNull(RecentValueHistory.firstSeenTestId(NS, "user-a", "v1"));
	}

	@Test
	void recordedValueReturnsItsTestId() {
		RecentValueHistory.record(NS, "user-a", "v1", "test-1");
		assertEquals("test-1", RecentValueHistory.firstSeenTestId(NS, "user-a", "v1"));
	}

	@Test
	void differentScopeDoesNotSeeAnotherScopesValue() {
		RecentValueHistory.record(NS, "user-a", "v1", "test-1");
		assertNull(RecentValueHistory.firstSeenTestId(NS, "user-b", "v1"));
	}

	@Test
	void differentNamespaceDoesNotSeeAnotherNamespacesValue() {
		RecentValueHistory.record(NS, "user-a", "v1", "test-1");
		assertNull(RecentValueHistory.firstSeenTestId("other-ns", "user-a", "v1"));
	}

	@Test
	void entryExpiresAfterRetentionWindow() throws InterruptedException {
		RecentValueHistory.setRetentionForTesting(Duration.ofMillis(50));
		RecentValueHistory.record(NS, "user-a", "v1", "test-1");
		assertEquals("test-1", RecentValueHistory.firstSeenTestId(NS, "user-a", "v1"));

		Thread.sleep(120);

		assertNull(RecentValueHistory.firstSeenTestId(NS, "user-a", "v1"));
	}

	@Test
	void checkAndRecordReturnsNullWhenUnseenAndRecordsAllValues() {
		assertNull(RecentValueHistory.checkAndRecord(NS, "user-a", List.of("v1", "v2"), "test-1"));
		assertEquals("test-1", RecentValueHistory.firstSeenTestId(NS, "user-a", "v1"));
		assertEquals("test-1", RecentValueHistory.firstSeenTestId(NS, "user-a", "v2"));
	}

	@Test
	void checkAndRecordReportsReuseAndRecordsNothing() {
		RecentValueHistory.record(NS, "user-a", "v1", "test-1");
		RecentValueHistory.SeenValue seen = RecentValueHistory.checkAndRecord(NS, "user-a", List.of("v2", "v1"), "test-2");
		assertEquals(new RecentValueHistory.SeenValue("v1", "test-1"), seen);
		// On reuse nothing must be recorded, including the unseen v2.
		assertNull(RecentValueHistory.firstSeenTestId(NS, "user-a", "v2"));
	}

	@Test
	void checkAndRecordDoesNotSelfTriggerOnDuplicateValuesInOneCall() {
		assertNull(RecentValueHistory.checkAndRecord(NS, "user-a", List.of("v1", "v1"), "test-1"));
		RecentValueHistory.SeenValue seen = RecentValueHistory.checkAndRecord(NS, "user-a", List.of("v1"), "test-2");
		assertEquals(new RecentValueHistory.SeenValue("v1", "test-1"), seen);
	}

	@Test
	void fifoCapDropsOldestEntries() {
		// Record one more than the per-scope cap; the oldest must be evicted, the newest retained.
		for (int i = 0; i <= RecentValueHistory.MAX_PER_SCOPE; i++) {
			RecentValueHistory.record(NS, "user-a", "v" + i, "test-" + i);
		}
		assertNull(RecentValueHistory.firstSeenTestId(NS, "user-a", "v0"));
		assertEquals("test-" + RecentValueHistory.MAX_PER_SCOPE,
			RecentValueHistory.firstSeenTestId(NS, "user-a", "v" + RecentValueHistory.MAX_PER_SCOPE));
	}
}
