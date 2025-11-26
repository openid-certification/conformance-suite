package net.openid.conformance.logging;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TraceService;
import net.openid.conformance.security.AuthenticationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * REST API for downloading Playwright trace files from test runs.
 */
@Controller
@RequestMapping(value = "/api")
public class TraceAPI {

	private static final Logger logger = LoggerFactory.getLogger(TraceAPI.class);

	@Autowired
	private TestInfoService testInfoService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TraceService traceService;

	@GetMapping(value = "/log/{id}/trace", produces = "application/zip")
	@Operation(summary = "Download Playwright trace for a test")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Trace downloaded successfully"),
		@ApiResponse(responseCode = "403", description = "You must be admin or test owner to download traces"),
		@ApiResponse(responseCode = "404", description = "No trace found for this test")
	})
	public ResponseEntity<byte[]> downloadTrace(
			@Parameter(description = "Id of test") @PathVariable(name = "id") String testId) {

		// Check authorization
		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);
		if (testOwner == null) {
			logger.debug("Test not found: {}", testId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!authenticationFacade.isAdmin() && !authenticationFacade.getPrincipal().equals(testOwner)) {
			logger.debug("Access denied for trace download, testId: {}", testId);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		// Get trace from service
		Optional<byte[]> traceOpt = traceService.getTraceForTestId(testId, authenticationFacade.isAdmin());

		if (traceOpt.isEmpty()) {
			logger.debug("No trace found for testId: {}", testId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		byte[] traceBytes = traceOpt.get();
		logger.info("Serving trace for testId: {}, size: {} bytes", testId, traceBytes.length);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/zip"));
		headers.setContentDispositionFormData("attachment", "trace-" + testId + ".zip");
		headers.setContentLength(traceBytes.length);

		return new ResponseEntity<>(traceBytes, headers, HttpStatus.OK);
	}
}
