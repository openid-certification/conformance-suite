package net.openid.conformance.logging;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncBatchingEventLogTest {

	@Mock
	private DBEventLog dbEventLog;

	private AsyncBatchingEventLog asyncEventLog;

	@BeforeEach
	void setUp() {
		// Small batch size and flush interval for testing
		asyncEventLog = new AsyncBatchingEventLog(dbEventLog, 1000, 5, 10, 5, 1000, 1, 10, 1);
		asyncEventLog.startWorker();
	}

	@Test
	void testLogAndFlush() {
		String testId = "test-123";
		String msg = "Hello, world!";
		Map<String, String> owner = Map.of("sub", "user1");
		Document doc = new Document("_id", "doc-1").append("testId", testId).append("msg", msg).append("testOwner", owner);

		when(dbEventLog.toDocument(eq(testId), anyString(), eq(owner), eq(msg), anyLong())).thenReturn(doc);

		asyncEventLog.log(testId, "test-source", owner, msg);

		// Check pending
		List<Document> pending = asyncEventLog.getPendingForTestId(testId);
		assertThat(pending).hasSize(1);
		assertThat(pending.get(0).getString("msg")).isEqualTo(msg);

		// Wait for flush
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
		verify(dbEventLog, timeout(1000)).insertDocuments(captor.capture());

		assertThat(captor.getValue()).contains(doc);

		// Pending should be empty now
		assertThat(asyncEventLog.getPendingForTestId(testId)).isEmpty();
	}
}
