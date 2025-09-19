package net.openid.conformance.openid.ssf.eventstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;

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

	protected Cache<String, BlockingQueue<OIDSSFSecurityEvent>> streamEventQueueCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	// streamId -> [jti]
	protected Cache<String, Set<String>> streamAcksAckedCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	// streamId -> {jti, Event}
	protected Cache<String, ConcurrentMap<String, OIDSSFSecurityEvent>> streamEventsCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	protected Cache<String, ConcurrentMap<String, JsonObject>> streamSetErrorsCache = CacheBuilder.newBuilder() //
		.expireAfterWrite(30, TimeUnit.MINUTES) // auto-remove entries 30min after insertion/update
		.build();

	@Override
	public void storeEvent(String streamId, OIDSSFSecurityEvent eventObject) {
		BlockingQueue<OIDSSFSecurityEvent> queue = getStreamEventQueue(streamId);

		queue.add(eventObject);
		getStreamEventsCache(streamId).put(eventObject.jti(), eventObject);
	}

	protected BlockingQueue<OIDSSFSecurityEvent> getStreamEventQueue(String streamId) {
		try {
			return streamEventQueueCache.get(streamId, () -> new ArrayBlockingQueue<>(MAX_CAPACITY));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	protected ConcurrentMap<String, OIDSSFSecurityEvent> getStreamEventsCache(String streamId) {
		try {
			return streamEventsCache.get(streamId, ConcurrentHashMap::new);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	protected Set<String> getStreamEventAcksAckedCache(String streamId) {
		try {
			return streamAcksAckedCache.get(streamId, ConcurrentSkipListSet::new);
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
	public EventsBatch pollEvents(String streamId, int maxCount, boolean waitForEvents, long waitTimeSeconds) {
		BlockingQueue<OIDSSFSecurityEvent> queue = getStreamEventQueue(streamId);

		if (queue==null) {
			return new EventsBatch(List.of(), false);
		}

		int maxCountClamped = Math.min(Math.max(maxCount, 0), MAX_CAPACITY);

		long startTime = System.currentTimeMillis();

		List<OIDSSFSecurityEvent> events = new ArrayList<>();
		int items = 0;
		boolean moreAvailable = false;
		while (!queue.isEmpty() || waitForEvents) {

			OIDSSFSecurityEvent event;

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

		return new EventsBatch(events, moreAvailable);
	}

	@Override
	public void purgeStreamEvents(String streamId) {
		BlockingQueue<OIDSSFSecurityEvent> queue = getStreamEventQueue(streamId);
		streamEventQueueCache.invalidate(streamId);
		if (queue != null) {
			queue.clear();
		}

		Set<String> streamAcks = getStreamEventAcksAckedCache(streamId);
		streamAcksAckedCache.invalidate(streamId);
		if (streamAcks != null) {
			streamAcks.clear();
		}

		ConcurrentMap<String, JsonObject> streamSetErrors = getStreamSetErrorsCache(streamId);
		streamSetErrorsCache.invalidate(streamId);
		if (streamSetErrors != null) {
			streamSetErrors.clear();
		}

		ConcurrentMap<String, OIDSSFSecurityEvent> streamAcksPending = getStreamEventsCache(streamId);
		streamEventsCache.invalidate(streamId);
		if (streamAcksPending != null) {
			streamAcksPending.clear();
		}
	}

	@Override
	public OIDSSFSecurityEvent registerAckForStreamEvent(String streamId, String jti) {
		// mark the jti as acked for the stream
		getStreamEventAcksAckedCache(streamId).add(jti);

		// return the stored pending event
		return getRegisteredSecurityEvent(streamId, jti);
	}

	@Override
	public OIDSSFSecurityEvent getRegisteredSecurityEvent(String streamId, String jti) {
		return getStreamEventsCache(streamId).get(jti);
	}

	@Override
	public boolean isStreamEventAcked(String streamId, String jti) {
		return getStreamEventAcksAckedCache(streamId).contains(jti);
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
