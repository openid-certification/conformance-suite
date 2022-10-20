package net.openid.conformance.logging;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@Tag(name = "ImageAPI", description = "A set of apis to handle screenshots and images on tests")
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
            @ApiResponse(responseCode = "403", description = "In order to upload an image, You must be admin or test owner")
    })
    public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
                                                           @Parameter(description = "Id of test") @PathVariable(name = "id") String testId,
                                                           @Parameter(description = "Description for image") @RequestParam(name = "description", required = false) String description) throws IOException {

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
    @Operation(summary = "Upload the image to existing log entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uploaded image successfully"),
            @ApiResponse(responseCode = "403", description = "In order to upload an image, You must be admin or test owner")
    })
    public ResponseEntity<Object> uploadImageToExistingLogEntry(
            @Parameter(description = "Image should be encoded as a string") @RequestBody String encoded,
            @Parameter(description = "Id of test") @PathVariable(name = "id") String testId,
            @Parameter(description = "Placeholder which created when the test run") @PathVariable(name = "placeholder") String placeholder) throws IOException {

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

}
