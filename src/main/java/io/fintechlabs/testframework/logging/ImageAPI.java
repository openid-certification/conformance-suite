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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.runner.TestRunnerSupport;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Result;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

/**
 * @author jricher
 *
 */
@Controller
public class ImageAPI {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TestInfoService testInfoService;

	@Autowired
	private TestRunnerSupport testRunnerSupport;
	
	@Autowired
	private AuthenticationFacade authenticationFacade;

	@PostMapping(path = "/log/{id}/imgfile")
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
		@PathVariable(name = "id") String testId) throws IOException {
		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		// Should this be checked? I.E. does a non-user facing client ever call this?
		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {
			// create a new entry in the database
			BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
				.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
				.add("testId", testId)
				.add("testOwner", testOwner)
				.add("src", "_image-api")
				.add("time", new Date().getTime())
				.add("img", encoded);

			mongoTemplate.insert(documentBuilder.get(), DBEventLog.COLLECTION);

			// an image was uploaded, it needs to be reviewed
			setTestReviewNeeded(testId);
		}

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PostMapping(path = "/log/{id}/imgfile/{placeholder}")
	public ResponseEntity<Object> uploadImageToExitingLogEntry(@RequestBody String encoded,
		@PathVariable(name = "id") String testId,
		@PathVariable(name = "placeholder") String placeholder) throws IOException {

		Criteria findTestId = Criteria.where("testId").is(testId);
		

		// add the placeholder condition
		Criteria placeholderExists = Criteria.where("upload").is(placeholder);

		// if we're not admin, make sure we also own the log
		Criteria criteria = createCriteria(findTestId, placeholderExists);

		Query query = Query.query(criteria);

		Update update = new Update();
		update.unset("upload");
		update.set("img", encoded);

		mongoTemplate.updateFirst(query, update, DBEventLog.COLLECTION);
		
		// an image was uploaded, it needs to be reviewed
		setTestReviewNeeded(testId);
		
		// check to see if all placeholders are set by searching for any remaining ones on this test
		Criteria noMorePlaceholders = Criteria.where("upload").exists(true);
		
		
		Criteria postSearch = createCriteria(findTestId, noMorePlaceholders);
		Query search = Query.query(postSearch);
		List<DBObject> remainingPlaceholders = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject()).toArray();
		
		if (remainingPlaceholders.size() == 0) {
			// there aren't any placeholders left on this test, update its status
			
			// first, see if it's currently running; if so we update the object directly
			TestModule test = testRunnerSupport.getRunningTestById(testId);
			if (test != null) {
				test.fireTestFinished();
			} else {
				// otherwise we need to do it directly
				testInfoService.updateTestStatus(testId, Status.FINISHED);
			}
		}

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	/**
	 * @param testId
	 */
	private void setTestReviewNeeded(String testId) {
		// first, see if it's currently running; if so we update the object directly
		TestModule test = testRunnerSupport.getRunningTestById(testId);
		if (test != null) {
			test.fireTestReviewNeeded();
		} else {
			// otherwise we need to do it directly
			testInfoService.updateTestResult(testId, Result.REVIEW);
		}		
	}

	// Create a Criteria with or without the security constraints as needed
	private Criteria createCriteria(Criteria findTestId, Criteria placeholderExists) {
		Criteria criteria = new Criteria();
		if (authenticationFacade.getAuthenticationToken() != null &&
			!authenticationFacade.isAdmin()) {
			criteria = criteria.andOperator(
				findTestId,
				placeholderExists,
				Criteria.where("testOwner").is(authenticationFacade.getPrincipal())
			);
		} else {
			criteria = criteria.andOperator(
				findTestId,
				placeholderExists
			);
		}
		return criteria;
	}

}
