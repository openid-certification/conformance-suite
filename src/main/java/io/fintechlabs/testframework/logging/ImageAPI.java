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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

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

	@PostMapping(path = "/log/{id}/images")
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
		@PathVariable(name = "id") String testId,
		@RequestParam(name = "description", required = false) String description) throws IOException {
		
		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			String entryId = testId + "-" + RandomStringUtils.randomAlphanumeric(32);
			
			// create a new entry in the database
			BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
				.add("_id", entryId)
				.add("testId", testId)
				.add("testOwner", testOwner)
				.add("src", "_image-api")
				.add("time", new Date().getTime())
				.add("msg", Strings.emptyToNull(description))
				.add("img", encoded);

			mongoTemplate.insert(documentBuilder.get(), DBEventLog.COLLECTION);
			
			DBObject updated = mongoTemplate.getCollection(DBEventLog.COLLECTION).findOne(entryId);

			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);
			return new ResponseEntity<>(updated, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

	}

	@PostMapping(path = "/log/{id}/images/{placeholder}")
	public ResponseEntity<Object> uploadImageToExitingLogEntry(@RequestBody String encoded,
		@PathVariable(name = "id") String testId,
		@PathVariable(name = "placeholder") String placeholder) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			Criteria findTestId = Criteria.where("testId").is(testId);

			// add the placeholder condition
			Criteria placeholderExists = Criteria.where("upload").is(placeholder);

			// if we're not admin, make sure we also own the log
			Criteria criteria = createCriteria(findTestId, placeholderExists);

			Query query = Query.query(criteria);

			Update update = new Update();
			update.unset("upload");
			update.set("img", encoded);

			DBObject result = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), DBObject.class, DBEventLog.COLLECTION);
			
			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);
			
			// check to see if all placeholders are set by searching for any remaining ones on this test
			Criteria noMorePlaceholders = Criteria.where("upload").exists(true);
			
			
			Criteria postSearch = createCriteria(findTestId, noMorePlaceholders);
			Query search = Query.query(postSearch);
			List<DBObject> remainingPlaceholders = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject()).toArray();
			
			if (remainingPlaceholders.size() == 0) {
				// there aren't any placeholders left on this test, update its status
				
				// first, see if it's currently running; if so we update the running object
				TestModule test = testRunnerSupport.getRunningTestById(testId);
				if (test != null) {
					test.fireTestFinished();		// set our current status to finished
					test.stop();					// stop the running test
				} else {
					// otherwise we need to do it directly in the database
					testInfoService.updateTestStatus(testId, Status.FINISHED);
				}
			}
			
			return new ResponseEntity<>(result, HttpStatus.OK);

		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		
	}
	
	@GetMapping(path = "/log/{id}/images", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getAllImages(@PathVariable(name = "id") String testId) {
		
		//db.EVENT_LOG.find({'testId': 'zpDg24jOXl', $or: [{img: {$exists: true}}, {upload: {$exists: true}}]}).sort({'time': 1})
		
		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {
			
			Criteria findTestId = Criteria.where("testId").is(testId);
			
			Criteria anyImages =
				new Criteria().orOperator(
					Criteria.where("img").exists(true),
					Criteria.where("upload").exists(true)
				);
			
			// add in the security parameters
			Criteria criteria = createCriteria(findTestId, anyImages);
			
			Query search = Query.query(criteria);
			
			List<DBObject> images = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject())
				.sort(BasicDBObjectBuilder.start()
					.add("time", 1)
					.get())
				.toArray();
			
			return new ResponseEntity<>(images, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		
	}

	/**
	 * @param testId
	 */
	private void setTestReviewNeeded(String testId) {
		// first, see if it's currently running; if so we update the running object
		TestModule test = testRunnerSupport.getRunningTestById(testId);
		if (test != null) {
			test.fireTestReviewNeeded();
		} else {
			// otherwise we need to do it directly in the database
			testInfoService.updateTestResult(testId, Result.REVIEW);
		}		
	}

	// Create a Criteria with or without the security constraints as needed
	private Criteria createCriteria(Criteria findTestId, Criteria additionalConstraints) {
		Criteria criteria = new Criteria();
		if (authenticationFacade.getAuthenticationToken() != null &&
			!authenticationFacade.isAdmin()) {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints,
				Criteria.where("testOwner").is(authenticationFacade.getPrincipal())
			);
		} else {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints
			);
		}
		return criteria;
	}

}
