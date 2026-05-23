package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DBEventLog implements EventLog {

	public static final String COLLECTION = "EVENT_LOG";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {
		Document doc = appendMetadata(new Document(), testId, source, owner);
		// Preserve the original string-log layout and its BSON-null handling for a null message.
		doc.append("msg", msg);
		insert(doc);
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
		write(testId, source, owner, jsonObjectToFieldMap(obj));
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
		write(testId, source, owner, map);
	}

	private void write(String testId, String source, Map<String, String> owner, Map<String, Object> fields) {
		Document doc = appendMetadata(fieldsToDocument(fields), testId, source, owner);
		insert(doc);
	}

	private static Document appendMetadata(Document doc, String testId, String source, Map<String, String> owner) {
		doc.append("_id", testId + "-" + RandomStringUtils.secure().nextAlphanumeric(32));
		doc.append("testId", testId);
		doc.append("src", source);
		doc.append("testOwner", owner);
		doc.append("time", new Date().getTime());
		return doc;
	}

	private void insert(Document doc) {
		mongoTemplate.insert(doc, COLLECTION);
	}

	/**
	 * Convert a caller's field map into the {@link Document} that will be handed to
	 * {@code mongoTemplate.insert}. The registered {@code MongoCustomConversions} handle the
	 * remaining Gson types ({@link JsonObject} / {@link com.google.gson.JsonArray} /
	 * {@link JsonPrimitive}) during the subsequent write, and key-wrapping for nested objects
	 * happens there too. Package-private so {@link BsonEncoding} can drive the exact same
	 * conversion path as production.
	 */
	static Document fieldsToDocument(Map<String, Object> fields) {
		return new Document(GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(fields));
	}

	/**
	 * Flatten a {@link JsonObject} into the field map consumed by {@link #write}, preserving the
	 * two behaviors that the previous {@code Document.parse(convertFieldsToStructure(obj).toString())}
	 * path provided at the top level:
	 *
	 * <ol>
	 *   <li>Top-level dotted, dollar, or already-wrapped keys are wrapped under a single
	 *   {@code __wrapped_key_element_*} envelope so {@code CollapsingGsonHttpMessageConverter}
	 *   restores them on read. Nested dotted keys inside {@link JsonObject} / {@link com.google.gson.JsonArray}
	 *   values are wrapped by the registered Mongo converters when they run — wrapping the whole
	 *   tree here would cause double-wrapping on those nested values.</li>
	 *   <li>Top-level {@link com.google.gson.JsonNull} values are replaced with the
	 *   {@code CONFORMANCE_SUITE_JSON_NULL} sentinel string. Nested JsonNulls are handled by
	 *   the registered converters; a JsonNull at the top level would otherwise reach
	 *   {@code MappingMongoConverter} and be reflected as a {@code {_class: "...JsonNull"}}
	 *   document.</li>
	 * </ol>
	 *
	 * Package-private so {@link BsonEncoding} can drive the exact same conversion path as production.
	 */
	static Map<String, Object> jsonObjectToFieldMap(JsonObject obj) {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
			String key = entry.getKey();
			JsonElement value = entry.getValue();
			if (value.isJsonNull()) {
				value = new JsonPrimitive(GsonObjectToBsonDocumentConverter.CONFORMANCE_SUITE_JSON_NULL_CONSTANT);
			}
			if (needsKeyWrapping(key)) {
				JsonObject wrap = new JsonObject();
				wrap.addProperty("key", key);
				wrap.add("value", value);
				fields.put("__wrapped_key_element_" + RandomStringUtils.secure().nextAlphabetic(6), wrap);
			} else {
				fields.put(key, value);
			}
		}
		return fields;
	}

	private static boolean needsKeyWrapping(String key) {
		return key.contains(".") || key.contains("$") || key.startsWith("__wrapped_key_element_");
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> eventLogCollection = mongoTemplate.getCollection(COLLECTION);
		eventLogCollection.createIndex(new Document("testId", 1));
		eventLogCollection.createIndex(new Document("testOwner", 1));
	}
}
