package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.WriteConcernError;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
@ConditionalOnProperty(name = "net.openid.conformance.logging.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncBatchingEventLog implements EventLog {
	private static final Logger logger = LoggerFactory.getLogger(AsyncBatchingEventLog.class);
	private static final int DUPLICATE_KEY_ERROR_CODE = 11000;
	private static final Set<Integer> NON_RETRYABLE_ERROR_CODES = Set.of(
		2,      // BadValue
		9,      // FailedToParse
		14,     // TypeMismatch
		121,    // DocumentValidationFailure
		10334   // BSONObjectTooLarge
	);

	private final DBEventLog dbEventLog;
	private final BlockingQueue<Document> queue;
	private final ConcurrentMap<String, ConcurrentMap<String, Document>> pendingByTestId = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, AtomicInteger> documentRetryCount = new ConcurrentHashMap<>();
	private final AtomicLong lastAssignedTime = new AtomicLong(0);
	private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

	private final int batchSize;
	private final int flushIntervalMs;
	private final int enqueueTimeoutMs;
	private final int shutdownDrainTimeoutMs;
	private final int bulkRetryCount;
	private final int retryBackoffMs;
	private final int maxDocumentRetries;

	private Thread workerThread;

	public AsyncBatchingEventLog(
		DBEventLog dbEventLog,
		@Value("${net.openid.conformance.logging.async.queue-capacity:20000}") int queueCapacity,
		@Value("${net.openid.conformance.logging.async.batch-size:100}") int batchSize,
		@Value("${net.openid.conformance.logging.async.flush-interval-ms:75}") int flushIntervalMs,
		@Value("${net.openid.conformance.logging.async.enqueue-timeout-ms:5}") int enqueueTimeoutMs,
		@Value("${net.openid.conformance.logging.async.shutdown-drain-timeout-ms:5000}") int shutdownDrainTimeoutMs,
		@Value("${net.openid.conformance.logging.async.bulk-retry-count:2}") int bulkRetryCount,
		@Value("${net.openid.conformance.logging.async.retry-backoff-ms:25}") int retryBackoffMs,
		@Value("${net.openid.conformance.logging.async.max-document-retries:3}") int maxDocumentRetries
	) {
		this.dbEventLog = dbEventLog;
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		this.batchSize = Math.max(1, batchSize);
		this.flushIntervalMs = Math.max(1, flushIntervalMs);
		this.enqueueTimeoutMs = Math.max(0, enqueueTimeoutMs);
		this.shutdownDrainTimeoutMs = Math.max(1, shutdownDrainTimeoutMs);
		this.bulkRetryCount = Math.max(0, bulkRetryCount);
		this.retryBackoffMs = Math.max(0, retryBackoffMs);
		this.maxDocumentRetries = Math.max(0, maxDocumentRetries);
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

		// Final drain: catch any documents enqueued after acceptingEvents was set to false
		// but before the producer saw the flag
		List<Document> stragglers = new ArrayList<>();
		queue.drainTo(stragglers);
		for (int i = 0; i < stragglers.size(); i += batchSize) {
			flushBatch(stragglers.subList(i, Math.min(i + batchSize, stragglers.size())));
		}
	}

	private void flushBatch(List<Document> batch) {
		for (int attempt = 0; attempt <= bulkRetryCount; attempt++) {
			try {
				dbEventLog.insertDocuments(batch);
				markPersisted(batch);
				return;
			} catch (MongoBulkWriteException bulkWriteException) {
				handleBulkWriteException(batch, bulkWriteException);
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
				markPersisted(document);
			} catch (RuntimeException exception) {
				if (isDuplicateKey(exception)) {
					markPersisted(document);
					continue;
				}
				handleDocumentFailure(document, null, exception.getMessage(), exception);
			}
		}
	}

	private void handleBulkWriteException(List<Document> batch, MongoBulkWriteException exception) {
		Map<Integer, BulkWriteError> writeErrorsByIndex = new HashMap<>();
		for (BulkWriteError error : exception.getWriteErrors()) {
			writeErrorsByIndex.put(error.getIndex(), error);
		}

		WriteConcernError writeConcernError = exception.getWriteConcernError();

		for (int i = 0; i < batch.size(); i++) {
			Document document = batch.get(i);
			BulkWriteError writeError = writeErrorsByIndex.get(i);

			if (writeError != null) {
				if (isDuplicateKey(writeError.getCode())) {
					markPersisted(document);
					continue;
				}
				handleDocumentFailure(document, writeError.getCode(), writeError.getMessage(), exception);
				continue;
			}

			if (writeConcernError != null) {
				handleDocumentFailure(document, writeConcernError.getCode(), writeConcernError.getMessage(), exception);
			} else {
				// With unordered insertMany, docs without write errors were persisted.
				markPersisted(document);
			}
		}
	}

	private void handleDocumentFailure(Document document, Integer errorCode, String errorMessage, RuntimeException exception) {
		if (isNonRetryableFailure(errorCode, errorMessage, exception)) {
			deadLetterAndMarkPersisted(document, "non_retryable", errorCode, errorMessage, currentRetryCount(document), exception);
			return;
		}

		String id = document.getString("_id");
		int retries = documentRetryCount.computeIfAbsent(id, ignored -> new AtomicInteger(0)).incrementAndGet();
		if (retries > maxDocumentRetries) {
			deadLetterAndMarkPersisted(document, "retry_exhausted", errorCode, errorMessage, retries, exception);
			return;
		}

		logger.warn("Failed to persist event log entry id={} testId={} (attempt {}/{}), re-queueing",
			id, document.getString("testId"), retries, maxDocumentRetries, exception);
		if (!tryRequeue(document)) {
			deadLetterAndMarkPersisted(document, "requeue_failed", errorCode, errorMessage, retries, exception);
		}
	}

	private boolean tryRequeue(Document document) {
		boolean queued = queue.offer(document);
		if (!queued && enqueueTimeoutMs > 0) {
			try {
				queued = queue.offer(document, enqueueTimeoutMs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return queued;
	}

	private int currentRetryCount(Document document) {
		AtomicInteger retries = documentRetryCount.get(document.getString("_id"));
		return retries == null ? 0 : retries.get();
	}

	private void deadLetterAndMarkPersisted(
		Document document,
		String reason,
		Integer errorCode,
		String errorMessage,
		int retryCount,
		RuntimeException cause
	) {
		try {
			dbEventLog.insertDeadLetter(document, reason, errorCode, errorMessage, retryCount);
		} catch (RuntimeException deadLetterException) {
			logger.error("Failed to write event-log dead-letter record for id={}", document.getString("_id"), deadLetterException);
		}
		logger.error("Moved event log entry id={} testId={} to dead-letter queue with reason={}",
			document.getString("_id"), document.getString("testId"), reason, cause);
		markPersisted(document);
	}

	private void markPersisted(List<Document> documents) {
		for (Document document : documents) {
			markPersisted(document);
		}
	}

	private void markPersisted(Document document) {
		removePending(document);
		documentRetryCount.remove(document.getString("_id"));
	}

	private boolean isNonRetryableFailure(Integer errorCode, String errorMessage, RuntimeException exception) {
		if (errorCode != null && NON_RETRYABLE_ERROR_CODES.contains(errorCode)) {
			return true;
		}

		String combined = ((errorMessage == null ? "" : errorMessage) + " "
			+ (exception.getMessage() == null ? "" : exception.getMessage())).toLowerCase(Locale.ROOT);

		return combined.contains("document failed validation")
			|| combined.contains("bsonobj size")
			|| combined.contains("object to insert too large")
			|| combined.contains("cannot encode")
			|| combined.contains("unknown bson type");
	}

	private void addPending(Document document) {
		String testId = document.getString("testId");
		String id = document.getString("_id");
		pendingByTestId.computeIfAbsent(testId, ignored -> new ConcurrentHashMap<>()).put(id, new Document(document));
	}

	private void removePending(Document document) {
		String testId = document.getString("testId");
		String id = document.getString("_id");
		pendingByTestId.computeIfPresent(testId, (key, perTestPending) -> {
			perTestPending.remove(id);
			return perTestPending.isEmpty() ? null : perTestPending;
		});
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

	private boolean isDuplicateKey(Integer errorCode) {
		return errorCode != null && errorCode == DUPLICATE_KEY_ERROR_CODE;
	}
}
