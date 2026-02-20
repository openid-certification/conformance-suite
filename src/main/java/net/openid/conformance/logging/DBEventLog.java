package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class DBEventLog implements EventLog {

	public static final String COLLECTION = "EVENT_LOG";

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
		return new Document()
			.append("_id", newLogId(testId))
			.append("testId", testId)
			.append("src", source)
			.append("testOwner", owner)
			.append("time", timestamp)
			.append("msg", msg);
	}

	Document toDocument(String testId, String source, Map<String, String> owner, JsonObject obj, long timestamp) {
		Document document = Document.parse(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(obj).toString()); // don't touch the incoming object
		document.append("_id", newLogId(testId));
		document.append("testId", testId);
		document.append("src", source);
		document.append("testOwner", owner);
		document.append("time", timestamp);
		return document;
	}

	Document toDocument(String testId, String source, Map<String, String> owner, Map<String, Object> map, long timestamp) {
		Document document = new Document(GsonArrayToBsonArrayConverter.convertUnloggableValuesInMap(map)); // this doesn't alter the incoming map
		document.append("_id", newLogId(testId));
		document.append("testId", testId);
		document.append("src", source);
		document.append("testOwner", owner);
		document.append("time", timestamp);
		return document;
	}

	void insertDocument(Document document) {
		mongoTemplate.insert(document, COLLECTION);
	}

	void insertDocumentsOrdered(List<Document> documents) {
		if (documents.isEmpty()) {
			return;
		}
		List<Document> copies = new ArrayList<>(documents.size());
		for (Document document : documents) {
			copies.add(new Document(document));
		}
		mongoTemplate.getCollection(COLLECTION).insertMany(copies, new InsertManyOptions().ordered(true));
	}

	private String newLogId(String testId) {
		return testId + "-" + RandomStringUtils.secure().nextAlphanumeric(32);
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> eventLogCollection = mongoTemplate.getCollection(COLLECTION);
		eventLogCollection.createIndex(new Document("testId", 1));
		eventLogCollection.createIndex(new Document("testOwner", 1));
		eventLogCollection.createIndex(new Document("testId", 1).append("time", 1));
		eventLogCollection.createIndex(new Document("testId", 1).append("testOwner", 1).append("time", 1));
	}
}
