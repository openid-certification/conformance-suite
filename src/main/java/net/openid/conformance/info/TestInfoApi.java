package net.openid.conformance.info;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping(value = "/api")
public class TestInfoApi {

	@Autowired
	private TestInfoRepository testInfos;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestInfoService testInfoService;

	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get information of all test module instances", description = "Will return all run test modules if user is admin role, otherwise only the logged in user's tests will be returned. This API is currently disabled due to performance concerns. If you have a need for it, please email details of your use case to " + AbstractCondition.SUPPORT_EMAIL)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAllTests() {
//		List<TestInfo> testInfo = null;
//		if (authenticationFacade.isAdmin()) {
//			testInfo = Lists.newArrayList(testInfos.findAll());
//		} else {
//			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
//			if (owner != null) {
//				testInfo = Lists.newArrayList(testInfos.findAllByOwner(owner));
//			}
//		}
//		return new ResponseEntity<>(testInfo, HttpStatus.OK);

		return new ResponseEntity<Object>("This API has been disabled due to performance concerns. If you have a need for it, please email details of your use case to " + AbstractCondition.SUPPORT_EMAIL, HttpStatus.BAD_REQUEST);
	}

	@GetMapping(value = "/info/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get test information by test id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Couldn't find test information for provided testId")
	})
	public ResponseEntity<Object> getTestInfo(
			@Parameter(description = "Id of test") @PathVariable String id,
			@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {

		Optional<?> testInfo = Optional.empty();
		if (publicOnly) {
			testInfo = testInfos.findByIdPublic(id);
		} else if (authenticationFacade.isAdmin()) {
			testInfo = testInfos.findById(id);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = testInfos.findByIdAndOwner(id, owner);
			}
		}
		if (testInfo.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(testInfo.get(), HttpStatus.OK);
		}

	}

	@PostMapping(value = "/info/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Publish a test information")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Published successfully"),
			@ApiResponse(responseCode = "400", description = "'publish' field is missing or its value is not JsonPrimitive"),
			@ApiResponse(responseCode = "403", description = "'publish' value is not valid")
	})
	public ResponseEntity<Object> publishTestInfo(
			@Parameter(description = "Id of test that you want to publish") @PathVariable String id,
			@Parameter(description = "Configuration Json") @RequestBody JsonObject config) {

		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		} else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!testInfoService.publishTest(id, publish)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("publish", publish);

		return new ResponseEntity<>(map, HttpStatus.OK);
	}

}
