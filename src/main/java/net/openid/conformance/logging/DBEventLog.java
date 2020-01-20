package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.MongoCollection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class DBEventLog implements EventLog {

	public static final String COLLECTION = "EVENT_LOG";

	private final MongoTemplate mongoTemplate;

	public DBEventLog(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	protected <T> void insert(T objectToSave) {
		mongoTemplate.insert(objectToSave, COLLECTION);
	}

	/* (non-Javadoc)
	 * @see EventLog#log(java.lang.String)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {

		Document document = new Document()
			.append("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
			.append("testId", testId)
			.append("src", source)
			.append("testOwner", owner)
			.append("time", new Date().getTime())
			.append("msg", msg);

		insert(document);
	}

	/* (non-Javadoc)
	 * @see EventLog#log(java.lang.String, com.google.gson.JsonObject)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {

		Document dbObject = Document.parse(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(obj).toString()); // don't touch the incoming object
		dbObject.append("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32));
		dbObject.append("testId", testId);
		dbObject.append("src", source);
		dbObject.append("testOwner", owner);
		dbObject.append("time", new Date().getTime());

		insert(dbObject);
	}

	/* (non-Javadoc)
	 * @see EventLog#log(java.lang.String, java.util.Map)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start(GsonArrayToBsonArrayConverter.convertAllValuesJsonArrayInMap(map)) // this doesn't alter the incoming map
			.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
			.add("testId", testId)
			.add("src", source)
			.add("testOwner", owner)
			.add("time", new Date().getTime());

		insert(documentBuilder.get());
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> eventLogCollection = mongoTemplate.getCollection(COLLECTION);
		eventLogCollection.createIndex(new Document("testId", 1));
		eventLogCollection.createIndex(new Document("testOwner", 1));
	}
}
