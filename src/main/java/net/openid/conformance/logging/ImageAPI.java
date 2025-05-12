package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestModule.Result;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
	@Operation(summary = "Upload image for a test log")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Uploaded image successfully"),
		@ApiResponse(responseCode = "400", description = "Image validation failure"),
		@ApiResponse(responseCode = "403", description = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
		@Parameter(description = "Id of test") @PathVariable(name = "id") String testId,
		@Parameter(description = "Description for image") @RequestParam(required = false) String description) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			// Validate encoded image.
			ResponseEntity<Object> response = validateEncodedImageFile(encoded, testId);

			if (response.getStatusCode() != HttpStatus.OK) {
				return response;
			}

			String entryId = testId + "-" + RandomStringUtils.secure().nextAlphanumeric(32);

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
	@Operation(summary = "Upload the image to existing log entry")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Uploaded image successfully"),
		@ApiResponse(responseCode = "400", description = "Image validation failure"),
		@ApiResponse(responseCode = "403", description = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> uploadImageToExistingLogEntry(
		@Parameter(description = "Image should be encoded as a string") @RequestBody String encoded,
		@Parameter(description = "Id of test") @PathVariable(name = "id") String testId,
		@Parameter(description = "Placeholder which created when the test run") @PathVariable String placeholder) throws IOException {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);

		if (authenticationFacade.isAdmin() ||
			authenticationFacade.getPrincipal().equals(testOwner)) {

			// Validate encoded image.
			ResponseEntity<Object> response = validateEncodedImageFile(encoded, testId);

			if (response.getStatusCode() != HttpStatus.OK) {
				return response;
			}

			Map<String, Object> update = ImmutableMap.of("img", encoded, "updatedAt", new Date().getTime());

			Document result = imageService.fillPlaceholder(testId, placeholder, update, false);

			// an image was uploaded, the test needs to be reviewed
			setTestReviewNeeded(testId);

			return new ResponseEntity<>(result, HttpStatus.OK);

		} else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

	}

	@GetMapping(path = "/log/{id}/images", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get all the images for a test")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "403", description = "In order to upload an image, You must be admin or test owner")
	})
	public ResponseEntity<Object> getAllImages(@Parameter(description = "ID of test") @PathVariable(name = "id") String testId) {

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

	/**
	 * Validate an encoded image file string.
	 *
	 * @param encoded  Image encoded as string
	 *
	 * @return A ResponseEntity object containing the result of the validation.
	 */
	private ResponseEntity<Object> validateEncodedImageFile(String encoded, String testId) {
		// Limit the number of uploaded images.
		Query query = new Query();
		query.addCriteria(Criteria.where("testId").is(testId));
		query.addCriteria(Criteria.where("img").exists(true));

		if (mongoTemplate.find(query, Document.class, DBEventLog.COLLECTION).size() >= 2) {
			return new ResponseEntity<Object>("Only 2 image uploads permitted per test", HttpStatus.BAD_REQUEST);
		}

		// Limit the accepted file types.
		final String[] imageTypes  = {"image/jpeg", "image/png"};
		boolean typeMatched = false;

		for (String type : imageTypes) {
			/*
			 * The encoded string is expected to start with the mime type.
			 * eg. 'data:image/png;'.
			 */
			if (encoded.startsWith("data:" + type + ";")) {
				typeMatched = true;
				break;
			}
		}

		if (! typeMatched) {
			return new ResponseEntity<Object>("Only jpeg/png files accepted", HttpStatus.BAD_REQUEST);
		}

		// Impose as 550KB file size limit.
		final int UPLOAD_SIZE_LIMIT = 500 * 1024;

		// Impose the limit on the file before encoding.
		final String encodingMarker = "base64,";
		int index = encoded.indexOf(encodingMarker);

		if (index != -1) {
			// Skip the file type header.
			String encodedData = encoded.substring(index + encodingMarker.length());

			byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
			if (decodedBytes.length > UPLOAD_SIZE_LIMIT) {
				return new ResponseEntity<Object>("File size exceeds the 500KB limit", HttpStatus.BAD_REQUEST);
			}
		}
		else {
			return new ResponseEntity<Object>("Invalid file encoding", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

}
