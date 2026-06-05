package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.openid.conformance.logging.MongoKeyWrapper.JSON_NULL_SENTINEL;
import static net.openid.conformance.logging.MongoKeyWrapper.buildEnvelope;
import static net.openid.conformance.logging.MongoKeyWrapper.needsWrapping;
import static net.openid.conformance.logging.MongoKeyWrapper.nextWrappedKey;

@Component
public class DBEventLog implements EventLog {

	public static final String COLLECTION = "EVENT_LOG";
	public static final String DEAD_LETTER_COLLECTION = "EVENT_LOG_DLQ";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {
		insertDocument(toDocument(testId, source, owner, msg));
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
		insertDocument(toDocument(testId, source, owner, obj));
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
		insertDocument(toDocument(testId, source, owner, map));
	}

	// The timestamp-less overloads stamp "now"; the timestamp-bearing overloads let
	// AsyncBatchingEventLog capture the event time at enqueue rather than at the
	// (deferred) flush. toDocument fully converts to a pure-BSON Document up front so
	// the queued documents are immutable snapshots and can be written via the raw
	// driver's bulk insertMany (see insertDocuments).

	Document toDocument(String testId, String source, Map<String, String> owner, String msg) {
		return toDocument(testId, source, owner, msg, new Date().getTime());
	}

	Document toDocument(String testId, String source, Map<String, String> owner, JsonObject obj) {
		return toDocument(testId, source, owner, obj, new Date().getTime());
	}

	Document toDocument(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
		return toDocument(testId, source, owner, map, new Date().getTime());
	}

	Document toDocument(String testId, String source, Map<String, String> owner, String msg, long timestamp) {
		// String messages carry no Gson types, so no converter pass is needed (this preserves the
		// original log(String) layout and its BSON-null handling for a null message); the raw driver
		// encodes the metadata + owner map directly.
		return appendMetadata(new Document(), testId, source, owner, timestamp).append("msg", msg);
	}

	Document toDocument(String testId, String source, Map<String, String> owner, JsonObject obj, long timestamp) {
		return materialize(appendMetadata(fieldsToDocument(jsonObjectToFieldMap(obj)), testId, source, owner, timestamp));
	}

	Document toDocument(String testId, String source, Map<String, String> owner, Map<String, Object> map, long timestamp) {
		return materialize(appendMetadata(fieldsToDocument(map), testId, source, owner, timestamp));
	}

	private static Document appendMetadata(Document doc, String testId, String source, Map<String, String> owner, long timestamp) {
		return doc
			.append("_id", newLogId(testId))
			.append("testId", testId)
			.append("src", source)
			.append("testOwner", owner)
			.append("time", timestamp);
	}

	/**
	 * Eagerly run the registered Mongo converters (Gson {@link JsonObject}/array/primitive values
	 * plus dotted-key wrapping) over {@code raw}, producing a pure-BSON snapshot. This is the same
	 * conversion {@code mongoTemplate.insert} performs lazily — done up front here so a batch of
	 * documents can sit on the async queue and be written via the raw driver's bulk
	 * {@code insertMany} (which has no Gson codecs) without mutating the caller's objects.
	 */
	private Document materialize(Document raw) {
		Document encoded = new Document();
		mongoTemplate.getConverter().write(raw, encoded);
		return encoded;
	}

	void insertDocument(Document document) {
		mongoTemplate.insert(document, COLLECTION);
	}

	void insertDocuments(List<Document> documents) {
		if (documents.isEmpty()) {
			return;
		}
		List<Document> copies = new ArrayList<>(documents.size());
		for (Document document : documents) {
			copies.add(new Document(document));
		}
		mongoTemplate.getCollection(COLLECTION).insertMany(copies, new InsertManyOptions().ordered(false));
	}

	void insertDeadLetter(Document failedDocument, String reason, Integer errorCode, String errorMessage, int retryCount) {
		Document deadLetter = new Document()
			.append("_id", failedDocument.getString("_id") + "-DLQ-" + RandomStringUtils.secure().nextAlphanumeric(8))
			.append("originalId", failedDocument.getString("_id"))
			.append("testId", failedDocument.getString("testId"))
			.append("src", failedDocument.getString("src"))
			.append("failedAt", new Date().getTime())
			.append("reason", reason)
			.append("errorCode", errorCode)
			.append("errorMessage", errorMessage)
			.append("retryCount", retryCount)
			.append("original", new Document(failedDocument));

		mongoTemplate.insert(deadLetter, DEAD_LETTER_COLLECTION);
	}

	private static String newLogId(String testId) {
		return testId + "-" + RandomStringUtils.secure().nextAlphanumeric(32);
	}

	/**
	 * Convert a caller's field map into a {@link Document}, leaving the Gson types for the
	 * registered {@code MongoCustomConversions}. Retained (package-private) so {@link BsonEncoding}
	 * can drive the same conversion path the project's other tests assert against. The production
	 * write path above converts eagerly via {@code toDocument} so that batched documents are
	 * immutable BSON snapshots; both produce equivalent stored documents.
	 */
	static Document fieldsToDocument(Map<String, Object> fields) {
		return new Document(GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(fields));
	}

	/**
	 * Flatten a {@link JsonObject} into the field map consumed by {@link #fieldsToDocument},
	 * wrapping top-level dotted/dollar keys under a {@code __wrapped_key_element_*} envelope and
	 * replacing top-level {@link com.google.gson.JsonNull} with the sentinel string. Package-private
	 * so {@link BsonEncoding} can drive the same conversion path.
	 */
	static Map<String, Object> jsonObjectToFieldMap(JsonObject obj) {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
			String key = entry.getKey();
			JsonElement value = entry.getValue();
			if (value.isJsonNull()) {
				value = new JsonPrimitive(JSON_NULL_SENTINEL);
			}
			if (needsWrapping(key)) {
				fields.put(nextWrappedKey(), buildEnvelope(key, value));
			} else {
				fields.put(key, value);
			}
		}
		return fields;
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> eventLogCollection = mongoTemplate.getCollection(COLLECTION);
		eventLogCollection.createIndex(new Document("testOwner", 1));
		eventLogCollection.createIndex(new Document("testId", 1).append("time", 1));
		eventLogCollection.createIndex(new Document("testId", 1).append("testOwner", 1).append("time", 1));

		MongoCollection<Document> deadLetterCollection = mongoTemplate.getCollection(DEAD_LETTER_COLLECTION);
		deadLetterCollection.createIndex(new Document("testId", 1));
		deadLetterCollection.createIndex(new Document("failedAt", -1));
	}
}
