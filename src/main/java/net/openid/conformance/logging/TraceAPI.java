package net.openid.conformance.logging;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TraceService;
import net.openid.conformance.security.AuthenticationFacade;
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
 * Download of the Playwright trace recorded for a test by the scripted browser
 * (only available when the suite runs with {@code -Dbrowser.engine=playwright} and tracing enabled).
 */
@Controller
@RequestMapping(value = "/api")
public class TraceAPI {

	private final TestInfoService testInfoService;

	private final AuthenticationFacade authenticationFacade;

	private final TraceService traceService;

	public TraceAPI(TestInfoService testInfoService, AuthenticationFacade authenticationFacade, TraceService traceService) {
		this.testInfoService = testInfoService;
		this.authenticationFacade = authenticationFacade;
		this.traceService = traceService;
	}

	@GetMapping(value = "/log/{id}/trace", produces = LogApi.APPLICATION_ZIP_VALUE)
	@Tag(name = SwaggerConfig.TAG_TEST_LOGS)
	@Operation(operationId = "downloadTestTrace", summary = "Download the Playwright browser trace for a test",
		description = "The trace archive can be opened with 'npx playwright show-trace <file>' or at https://trace.playwright.dev/. "
			+ "Only recorded when the suite's scripted browser is Playwright with tracing enabled.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Trace archive",
			content = @Content(mediaType = LogApi.APPLICATION_ZIP_VALUE, schema = @Schema(type = "string", format = "binary"))),
		@ApiResponse(responseCode = "403", description = "You must be admin or the test owner to download the trace", content = @Content),
		@ApiResponse(responseCode = "404", description = "Unknown test id, or no trace was recorded for the test", content = @Content)
	})
	public ResponseEntity<byte[]> downloadTrace(@Parameter(description = "Id of test") @PathVariable(name = "id") String testId) {

		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);
		if (testOwner == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!authenticationFacade.isAdmin() && !authenticationFacade.getPrincipal().equals(testOwner)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Optional<byte[]> trace = traceService.getTraceForTestId(testId);
		if (trace.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(LogApi.APPLICATION_ZIP_VALUE));
		headers.add("Content-Disposition", "attachment; filename=\"trace-" + testId + ".zip\"");
		headers.setContentLength(trace.get().length);

		return new ResponseEntity<>(trace.get(), headers, HttpStatus.OK);
	}
}
