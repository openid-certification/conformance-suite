package net.openid.conformance.logging;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.openid.conformance.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestModule.Result;

@Controller
@RequestMapping(value = "/api")
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
	@ApiOperation(value = "Upload image for a test log")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Uploaded image successfully"),
		@ApiResponse(code = 403, message = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
		@ApiParam(value = "Id of test") @PathVariable(name = "id") String testId,
		@ApiParam(value = "Description for image") @RequestParam(name = "description", required = false) String description) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			String entryId = testId + "-" + RandomStringUtils.randomAlphanumeric(32);

			// create a new entry in the database
			Document document = new Document()
				.append("_id", entryId)
				.append("testId", testId)
				.append("testOwner", testOwner)
				.append("src", "_image-api")
				.append("time", new Date().getTime())
				.append("msg", Strings.emptyToNull(description))
				.append("img", encoded);

			mongoTemplate.insert(document, DBEventLog.COLLECTION);

			Document updated = mongoTemplate.findById(entryId, Document.class, DBEventLog.COLLECTION);

			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);
			return new ResponseEntity<>(updated, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

	}

	@PostMapping(path = "/log/{id}/images/{placeholder}")
	@ApiOperation(value = "Upload the image to existing log entry")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Uploaded image successfully"),
		@ApiResponse(code = 403, message = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> uploadImageToExistingLogEntry(
		@ApiParam(value = "Image should be encoded as a string") @RequestBody String encoded,
		@ApiParam(value = "Id of test") @PathVariable(name = "id") String testId,
		@ApiParam(value = "Placeholder which created when the test run") @PathVariable(name = "placeholder") String placeholder) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			Map<String, Object> update = ImmutableMap.of("img", encoded, "updatedAt", new Date().getTime());

			Document result = imageService.fillPlaceholder(testId, placeholder, update, false);

			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);

			return new ResponseEntity<>(result, HttpStatus.OK);

		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

	}

	@GetMapping(path = "/log/{id}/images", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ApiOperation(value = "Get all the images for a test")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 403, message = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> getAllImages(@ApiParam(value = "ID of test") @PathVariable(name = "id") String testId) {

		//db.EVENT_LOG.find({'testId': 'zpDg24jOXl', $or: [{img: {$exists: true}}, {upload: {$exists: true}}]}).sort({'time': 1})

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			List<Document> images = imageService.getAllImagesForTestId(testId, false);

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
