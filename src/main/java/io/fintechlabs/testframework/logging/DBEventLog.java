/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.logging;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import io.fintechlabs.testframework.info.TestInfoService;

/**
 * @author jricher
 *
 */
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

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
			.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
			.add("testId", testId)
			.add("src", source)
			.add("testOwner", owner)
			.add("time", new Date().getTime())
			.add("blockId", blockId)
			.add("msg", msg);

		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, com.google.gson.JsonObject)
	 */
	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {

		DBObject dbObject = (DBObject) JSON.parse(GsonObjectToBsonDocumentConverter.convertFieldsToStructure(obj).toString()); // don't touch the incoming object
		dbObject.put("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32));
		dbObject.put("testId", testId);
		dbObject.put("src", source);
		dbObject.put("testOwner", owner);
		dbObject.put("time", new Date().getTime());
		dbObject.put("blockId", blockId);

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
	
}
