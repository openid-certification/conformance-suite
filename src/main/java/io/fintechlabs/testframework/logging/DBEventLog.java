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

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author jricher
 *
 */
@Component
public class DBEventLog implements EventLog {

	private static final String COLLECTION = "EVENT_LOG";
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String)
	 */
	@Override
	public void log(String source, String msg) {
		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
				.add("src", source)
				.add("time", new Date().getTime())
				.add("msg", msg);
		
		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.logging.EventLog#log(java.lang.String, com.google.gson.JsonObject)
	 */
	@Override
	public void log(String source, JsonObject obj) {
		obj.addProperty("src", source);
		obj.addProperty("time", new Date().getTime());

		DBObject dbObject = (DBObject) JSON.parse(obj.toString());
		
		mongoTemplate.insert(dbObject, COLLECTION);
	}

}
