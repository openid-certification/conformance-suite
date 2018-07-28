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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.logging.DBEventLog;
import io.fintechlabs.testframework.security.AuthenticationFacade;

/**
 * @author jricher
 *
 */
@Service
public class DBTestPlanService implements TestPlanService {

	public static final String COLLECTION = "TEST_PLAN";

	private static Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/**
	 * @param planId
	 * @param testName
	 * @param id
	 */
	@Override
	public void updateTestPlanWithModule(String planId, String testName, String id) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(planId);
		criteria.and("modules.testModule").is(testName);

		Query query = new Query(criteria);

		Update update = new Update();
		update.push("modules.$.instances", id);

		mongoTemplate.updateFirst(query, update, COLLECTION);


	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#createTestPlan(java.lang.String, java.lang.String, com.google.gson.JsonObject, java.util.Map, io.fintechlabs.testframework.plan.TestPlan)
	 */
	@Override
	public void createTestPlan(String id, String planName, JsonObject config, String description, String[] testModules) {

		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
			.add("_id", id)
			.add("planName", planName)
			.add("config", config)
			.add("started", Instant.now().toString())
			.add("owner", owner)
			.add("description", description)
			.add("version", version);

		List<DBObject> moduleStructure = new ArrayList<>();

		for (String module : testModules) {
			BasicDBObjectBuilder moduleBuilder = BasicDBObjectBuilder.start()
				.add("testModule", module)
				.add("instances", Collections.emptyList());

			moduleStructure.add(moduleBuilder.get());
		}

		documentBuilder.add("modules", moduleStructure);


		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getTestPlan(java.lang.String)
	 */
	@Override
	public Map getTestPlan(String id) {
		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		Query query = new Query(criteria);

		DBObject testPlan = mongoTemplate.getCollection(COLLECTION).findOne(query.getQueryObject());

		if (testPlan == null) {
			return null;
		} else {
			return testPlan.toMap();
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getAllPlansForCurrentUser()
	 */
	@Override
	public List<Map> getAllPlansForCurrentUser() {

		Criteria criteria = new Criteria();

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		Query query = new Query(criteria);

		List<DBObject> results = mongoTemplate.getCollection(COLLECTION).find(query.getQueryObject()).toArray();

		return results.stream().map(e -> e.toMap()).collect(Collectors.toList());

	}


}
