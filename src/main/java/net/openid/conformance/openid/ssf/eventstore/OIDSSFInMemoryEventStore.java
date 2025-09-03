package net.openid.conformance.openid.ssf.eventstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OIDSSFInMemoryEventStore implements OIDSSFEventStore {

	public static final int MAX_CAPACITY = 1000;

	protected Cache<String, BlockingQueue<JsonObject>> streamEventsCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	protected Cache<String, Set<String>> streamAcksCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	protected Cache<String, ConcurrentMap<String, JsonObject>> streamSetErrorsCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	@Override
	public void storeEvent(String streamId, JsonObject eventObject) {
		BlockingQueue<JsonObject> queue = getStreamEventQueue(streamId);

		queue.add(eventObject);
	}

	protected BlockingQueue<JsonObject> getStreamEventQueue(String streamId) {
		try {
			return streamEventsCache.get(streamId, () -> new ArrayBlockingQueue<>(MAX_CAPACITY));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	protected Set<String> getStreamAcksCache(String streamId) {
		try {
			return streamAcksCache.get(streamId, ConcurrentSkipListSet::new);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	protected ConcurrentMap<String, JsonObject> getStreamSetErrorsCache(String streamId) {
		try {
			return streamSetErrorsCache.get(streamId, ConcurrentHashMap::new);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PollInfo pollEvents(String streamId, int maxCount, boolean waitForEvents, long waitTimeSeconds) {
		BlockingQueue<JsonObject> queue = getStreamEventQueue(streamId);

		int maxCountClamped = Math.min(Math.max(maxCount, 0), MAX_CAPACITY);

		long startTime = System.currentTimeMillis();

		List<JsonObject> events = new ArrayList<>();
		int items = 0;
		boolean moreAvailable = false;
		while (!queue.isEmpty() || waitForEvents) {

			JsonObject event;

			if (waitForEvents) {
				try {
					event = queue.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					event = null;
				}
			} else {
				event = queue.poll();
			}

			if (event != null) {
				events.add(event);
				items++;
			}

			moreAvailable = queue.peek() != null;
			if (items >= maxCountClamped) {
				break;
			}

			// we waited long enough return with the data that we have
			long durationSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
			if (durationSeconds > waitTimeSeconds) {
				break;
			}
		}

		return new PollInfo(events, moreAvailable);
	}

	@Override
	public void purgeStreamEvents(String streamId) {
		BlockingQueue<JsonObject> queue = getStreamEventQueue(streamId);
		streamEventsCache.invalidate(streamId);
		if (queue != null) {
			queue.clear();
		}

		Set<String> streamAcks = getStreamAcksCache(streamId);
		streamAcksCache.invalidate(streamId);
		if (streamAcks != null) {
			streamAcks.clear();
		}

		ConcurrentMap<String, JsonObject> streamSetErrors = getStreamSetErrorsCache(streamId);
		streamSetErrorsCache.invalidate(streamId);
		if (streamSetErrors != null) {
			streamSetErrors.clear();
		}
	}

	@Override
	public void registerAckForStreamEvent(String streamId, String jti) {
		getStreamAcksCache(streamId).add(jti);
	}

	@Override
	public boolean isStreamEventAck(String streamId, String jti) {
		return getStreamAcksCache(streamId).contains(jti);
	}

	@Override
	public void registerErrorForStreamEvent(String streamId, String jti, JsonObject error) {
		getStreamSetErrorsCache(streamId).put(jti, error);
	}

	@Override
	public JsonObject isErrorForStreamEvent(String streamId, String jti) {
		return getStreamSetErrorsCache(streamId).get(jti);
	}
}
