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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;

import io.fintechlabs.testframework.plan.TestPlan;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Result;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

/**
 * @author jricher
 *
 */
@Service
public class DBTestInfoService implements TestInfoService {

	public static final String COLLECTION = "TEST_INFO";
	
	private static Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;
	
	@Autowired
	private TestPlanService testPlanService;

	//Private cache for holding test owners without having to hit the db
	LoadingCache<String, ImmutableMap<String, String>> testOwnerCache = CacheBuilder.newBuilder()
		.maximumSize(1000)
		.expireAfterAccess(30, TimeUnit.MINUTES) // is 30 minutes a good time out? too much? too little?
		.build(
			new CacheLoader<String, ImmutableMap<String, String>>() {
				@Override
				public ImmutableMap<String, String> load(String key) {
					Query query = Query.query(Criteria.where("_id").is(key));
					BasicDBObject test = mongoTemplate.findOne(query, BasicDBObject.class, COLLECTION);
					if (test != null &&
						test.containsField("owner")) {
						BasicDBObject owner = (BasicDBObject) test.get("owner");
						String iss = owner.getString("iss");
						String sub = owner.getString("sub");
						return ImmutableMap.of("sub", sub, "iss", iss);
					}
					return null;
				}
			});

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#createTest(java.lang.String, java.lang.String, java.lang.String, com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void createTest(String id, String testName, String url, JsonObject config, String alias, Instant started, String planId) {
		OIDCAuthenticationToken token = authenticationFacade.getAuthenticationToken();
		ImmutableMap<String, String> owner = null;
		if (token != null) {
			owner = (ImmutableMap<String, String>) token.getPrincipal();
		}
		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
			.add("_id", id)
			.add("testId", id)
			.add("testName", testName)
			.add("started", started.toString())
			.add("config", config)
			.add("alias", alias)
			.add("owner", owner)
			.add("planId", planId)
			.add("status", TestModule.Status.CREATED)
			.add("version", version);

		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
		
		if (planId != null) {
			testPlanService.updateTestPlanWithModule(planId, testName, id);
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#updateTestResult(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule.Result)
	 */
	@Override
	public void updateTestResult(String id, Result result) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);
		// find the existing entity
		//Query query = Query.query(
		//		Criteria.where("_id").is(id));

		// if there is a user logged in who isn't an admin, limit the search
		if (authenticationFacade.getAuthenticationToken() != null &&
			!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
			//query.addCriteria(Criteria.where("owner").is(authenticationFacade.getPrincipal()));
		}

		Query query = new Query(criteria);

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
		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		//Query query = Query.query(
		//		Criteria.where("_id").is(id));

		// if there is a user logged in who isn't an admin, limit the search
		if (authenticationFacade.getAuthenticationToken() != null &&
			!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
			//query.addCriteria(Criteria.where("owner").is(authenticationFacade.getPrincipal()));
		}

		Query query = new Query(criteria);

		Update update = new Update();
		update.set("status", status);

		mongoTemplate.updateFirst(query, update, COLLECTION);

	}

	@Override
	public ImmutableMap<String, String> getTestOwner(String testId) {
		try {
			return testOwnerCache.get(testId);
		} catch (ExecutionException e) {
			logger.error("ExecutionException while looking up owner for testId: " + testId, e);
		}
		return null;

		/* Non caching code here
		Query query = Query.query(Criteria.where("_id").is(id));
		BasicDBObject test = mongoTemplate.findOne(query, BasicDBObject.class, COLLECTION);
		if (test != null &&
				test.containsField("owner")) {
			BasicDBObject owner = (BasicDBObject)test.get("owner");
			String iss = owner.getString("iss");
			String sub = owner.getString("sub");
			return ImmutableMap.of("sub", sub, "iss", iss);
		} else {
			return null;
		}
		*/
	}

}
