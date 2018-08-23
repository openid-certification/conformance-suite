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
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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

import io.fintechlabs.testframework.info.ImageService;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.runner.TestRunnerSupport;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Result;

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

	@Autowired
	private ImageService imageService;

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
	public ResponseEntity<Object> uploadImageToExistingLogEntry(@RequestBody String encoded,
		@PathVariable(name = "id") String testId,
		@PathVariable(name = "placeholder") String placeholder) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			Map<String, String> update = ImmutableMap.of("img", encoded);

			DBObject result = imageService.fillPlaceholder(testId, placeholder, update, false);

			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);

			List<DBObject> remainingPlaceholders = imageService.getRemainingPlaceholders(testId, false);

			if (remainingPlaceholders.size() == 0) {
				imageService.lastPlaceholderFilled(testId, false);
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

			List<DBObject> images = imageService.getAllImagesForTestId(testId, false);

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

}
