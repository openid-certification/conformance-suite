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
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;

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
				.add("time", new Date().getTime())
				.add("config", config)
				.add("alias", alias)
				.add("status", Status.CREATED)
				.add("result", Result.UNKNOWN);
		
		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

}
