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

package io.fintechlabs.testframework.info;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;

import io.fintechlabs.testframework.logging.DBEventLog;
import io.fintechlabs.testframework.testmodule.TestModule.Result;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

/**
 * @author jricher
 *
 */
@Service
public class DBTestInfoService implements TestInfoService {

	public static final String COLLECTION = "TEST_INFO";
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#createTest(java.lang.String, java.lang.String, java.lang.String, com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void createTest(String id, String testName, String url, JsonObject config, String alias) {
		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
				.add("_id", id)
				.add("testId", id)
				.add("testName", testName)
				.add("started", new Date().getTime())
				.add("config", config)
				.add("alias", alias);
		
		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#updateTestResult(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule.Result)
	 */
	@Override
	public void updateTestResult(String id, Result result) {
		// find the existing entity
		Query query = Query.query(
				Criteria.where("_id").is(id));
		
		Update update = new Update();
		update.set("result", result);

		mongoTemplate.updateFirst(query, update, COLLECTION);

		
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#updateTestStatus(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule.Status)
	 */
	@Override
	public void updateTestStatus(String id, Status status) {
		// find the existing entity
		Query query = Query.query(
				Criteria.where("_id").is(id));
		
		Update update = new Update();
		update.set("status", status);

		mongoTemplate.updateFirst(query, update, COLLECTION);
		
	}

}
