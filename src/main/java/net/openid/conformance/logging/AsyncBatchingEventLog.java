package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
@ConditionalOnProperty(name = "net.openid.conformance.logging.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncBatchingEventLog implements EventLog {
	private static final Logger logger = LoggerFactory.getLogger(AsyncBatchingEventLog.class);
	private static final int DUPLICATE_KEY_ERROR_CODE = 11000;

	private final DBEventLog dbEventLog;
	private final BlockingQueue<Document> queue;
	private final ConcurrentMap<String, ConcurrentMap<String, Document>> pendingByTestId = new ConcurrentHashMap<>();
	private final AtomicLong lastAssignedTime = new AtomicLong(0);
	private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

	private final int batchSize;
	private final int flushIntervalMs;
	private final int enqueueTimeoutMs;
	private final int shutdownDrainTimeoutMs;
	private final int bulkRetryCount;
	private final int retryBackoffMs;

	private Thread workerThread;

	public AsyncBatchingEventLog(
		DBEventLog dbEventLog,
		@Value("${net.openid.conformance.logging.async.queue-capacity:20000}") int queueCapacity,
		@Value("${net.openid.conformance.logging.async.batch-size:100}") int batchSize,
		@Value("${net.openid.conformance.logging.async.flush-interval-ms:75}") int flushIntervalMs,
		@Value("${net.openid.conformance.logging.async.enqueue-timeout-ms:5}") int enqueueTimeoutMs,
		@Value("${net.openid.conformance.logging.async.shutdown-drain-timeout-ms:5000}") int shutdownDrainTimeoutMs,
		@Value("${net.openid.conformance.logging.async.bulk-retry-count:2}") int bulkRetryCount,
		@Value("${net.openid.conformance.logging.async.retry-backoff-ms:25}") int retryBackoffMs
	) {
		this.dbEventLog = dbEventLog;
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		this.batchSize = Math.max(1, batchSize);
		this.flushIntervalMs = Math.max(1, flushIntervalMs);
		this.enqueueTimeoutMs = Math.max(0, enqueueTimeoutMs);
		this.shutdownDrainTimeoutMs = Math.max(1, shutdownDrainTimeoutMs);
		this.bulkRetryCount = Math.max(0, bulkRetryCount);
		this.retryBackoffMs = Math.max(0, retryBackoffMs);
	}

	@PostConstruct
	public void startWorker() {
		workerThread = new Thread(this::runWorker, "event-log-batcher");
		workerThread.setDaemon(true);
		workerThread.start();
	}

	@PreDestroy
	public void stopWorker() {
		acceptingEvents.set(false);
		if (workerThread == null) {
			return;
		}
		try {
			workerThread.join(shutdownDrainTimeoutMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (workerThread.isAlive()) {
			logger.warn("Timed out waiting for async event log writer to flush outstanding entries");
			workerThread.interrupt();
		}
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {
		enqueueOrFallback(dbEventLog.toDocument(testId, source, owner, msg, nextMonotonicTime()));
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
		enqueueOrFallback(dbEventLog.toDocument(testId, source, owner, obj, nextMonotonicTime()));
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
		enqueueOrFallback(dbEventLog.toDocument(testId, source, owner, map, nextMonotonicTime()));
	}

	@Override
	public void createIndexes() {
		dbEventLog.createIndexes();
	}

	List<Document> getPendingForTestId(String testId) {
		ConcurrentMap<String, Document> pending = pendingByTestId.get(testId);
		if (pending == null || pending.isEmpty()) {
			return List.of();
		}
		List<Document> snapshot = new ArrayList<>(pending.size());
		for (Document document : pending.values()) {
			snapshot.add(new Document(document));
		}
		return snapshot;
	}

	private void enqueueOrFallback(Document document) {
		if (!acceptingEvents.get()) {
			dbEventLog.insertDocument(document);
			return;
		}

		addPending(document);
		boolean queued = queue.offer(document);
		if (!queued && enqueueTimeoutMs > 0) {
			try {
				queued = queue.offer(document, enqueueTimeoutMs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (queued) {
			return;
		}

		removePending(document);
		logger.warn("Async event log queue is full, falling back to synchronous insert for testId={}", document.getString("testId"));
		dbEventLog.insertDocument(document);
	}

	private void runWorker() {
		while (acceptingEvents.get() || !queue.isEmpty()) {
			try {
				Document first = queue.poll(100, TimeUnit.MILLISECONDS);
				if (first == null) {
					continue;
				}
				List<Document> batch = new ArrayList<>(batchSize);
				batch.add(first);

				long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushIntervalMs);
				while (batch.size() < batchSize) {
					long nanosRemaining = deadline - System.nanoTime();
					if (nanosRemaining <= 0) {
						break;
					}
					Document next = queue.poll(nanosRemaining, TimeUnit.NANOSECONDS);
					if (next == null) {
						break;
					}
					batch.add(next);
				}

				flushBatch(batch);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void flushBatch(List<Document> batch) {
		for (int attempt = 0; attempt <= bulkRetryCount; attempt++) {
			try {
				dbEventLog.insertDocumentsOrdered(batch);
				for (Document document : batch) {
					removePending(document);
				}
				return;
			} catch (RuntimeException exception) {
				if (attempt == bulkRetryCount) {
					logger.warn("Bulk event log insert failed after {} attempts, degrading to per-document inserts", attempt + 1, exception);
					break;
				}
				if (retryBackoffMs > 0) {
					try {
						Thread.sleep((long) retryBackoffMs * (attempt + 1));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		}

		for (Document document : batch) {
			try {
				dbEventLog.insertDocument(document);
				removePending(document);
			} catch (RuntimeException exception) {
				if (isDuplicateKey(exception)) {
					removePending(document);
					continue;
				}
				logger.error("Failed to persist event log entry id={} testId={}, re-queueing", document.getString("_id"), document.getString("testId"), exception);
				requeueDocument(document);
			}
		}
	}

	private void requeueDocument(Document document) {
		boolean queued = queue.offer(document);
		if (!queued && enqueueTimeoutMs > 0) {
			try {
				queued = queue.offer(document, enqueueTimeoutMs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (!queued) {
			logger.error("Unable to re-queue event log entry id={} due to queue capacity limits", document.getString("_id"));
		}
	}

	private void addPending(Document document) {
		String testId = document.getString("testId");
		String id = document.getString("_id");
		pendingByTestId.computeIfAbsent(testId, ignored -> new ConcurrentHashMap<>()).put(id, new Document(document));
	}

	private void removePending(Document document) {
		String testId = document.getString("testId");
		String id = document.getString("_id");
		ConcurrentMap<String, Document> perTestPending = pendingByTestId.get(testId);
		if (perTestPending == null) {
			return;
		}
		perTestPending.remove(id);
		if (perTestPending.isEmpty()) {
			pendingByTestId.remove(testId, perTestPending);
		}
	}

	private long nextMonotonicTime() {
		long now = System.currentTimeMillis();
		return lastAssignedTime.accumulateAndGet(now, (previous, current) -> current > previous ? current : previous + 1);
	}

	private boolean isDuplicateKey(RuntimeException exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof MongoWriteException mongoWriteException
				&& mongoWriteException.getError() != null
				&& mongoWriteException.getError().getCode() == DUPLICATE_KEY_ERROR_CODE) {
				return true;
			}

			if (current instanceof MongoBulkWriteException mongoBulkWriteException
				&& mongoBulkWriteException.getWriteErrors().stream().anyMatch(error -> error.getCode() == DUPLICATE_KEY_ERROR_CODE)) {
				return true;
			}

			if (current.getMessage() != null && current.getMessage().contains("E11000")) {
				return true;
			}

			current = current.getCause();
		}
		return false;
	}
}
