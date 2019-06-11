package io.fintechlabs.testframework.logging;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.MongoCollection;

import io.fintechlabs.testframework.info.TestInfoService;

@Component
public class DBEventLog implements EventLog {

	private static final Logger log = LoggerFactory.getLogger(DBEventLog.class);

	public static final String COLLECTION = "EVENT_LOG";

	// a block identifier for a log entry
	private String blockId = null;

	// random number generator
	private Random random = new SecureRandom();

	@Autowired
	private TestInfoService testInfoService;

	@Autowired
	private MongoTemplate mongoTemplate;

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {

		Document document = new Document()
			.append("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
			.append("testId", testId)
			.append("src", source)
			.append("testOwner", owner)
			.append("time", new Date().getTime())
			.append("blockId", blockId)
			.append("msg", msg);

		mongoTemplate.insert(document, COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, com.google.gson.JsonObject)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {

		Document dbObject = Document.parse(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(obj).toString()); // don't touch the incoming object
		dbObject.append("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32));
		dbObject.append("testId", testId);
		dbObject.append("src", source);
		dbObject.append("testOwner", owner);
		dbObject.append("time", new Date().getTime());
		dbObject.append("blockId", blockId);

		mongoTemplate.insert(dbObject, COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, java.util.Map)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start(map) // this doesn't alter the incoming map
			.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
			.add("testId", testId)
			.add("src", source)
			.add("testOwner", owner)
			.add("time", new Date().getTime())
			.add("blockId", blockId);

		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	@Override
	public String startBlock() {
		// create a random six-character hex string that we can use as a CSS color code in the logs
		blockId = Strings.padStart(
			Integer.toHexString(
				random.nextInt(256 * 256 * 256))
			, 6, '0');



		return blockId;
	}

	@Override
	public String endBlock() {
		String oldBlock = blockId;
		blockId = null;
		return oldBlock;
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> eventLogCollection = mongoTemplate.getCollection(COLLECTION);
		eventLogCollection.createIndex(new Document("testId", 1));
		eventLogCollection.createIndex(new Document("testOwner", 1));
	}
}
