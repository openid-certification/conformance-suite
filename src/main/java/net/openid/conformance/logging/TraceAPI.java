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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Playwright traces recorded for a test by the scripted browser, one per scripted browser
 * visit (only recorded when the suite runs with {@code browser.engine=playwright} and tracing
 * enabled).
 *
 * <p>A trace holds screenshots and DOM snapshots of every page the browser visited, login forms
 * included, so both endpoints are for the test's owner or an admin only: never public, and not
 * reachable through a share link.
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

	@GetMapping(value = "/log/{id}/traces", produces = MediaType.APPLICATION_JSON_VALUE)
	@Tag(name = SwaggerConfig.TAG_TEST_LOGS)
	@Operation(operationId = "listTestTraces", summary = "List the Playwright browser traces recorded for a test",
		description = "One entry per scripted browser visit that kept a trace: 'visit' is the number of the visit within the test "
			+ "(from 1, in the order the test requested them) and 'size' the size of the archive in bytes. "
			+ "Empty when no trace was recorded.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "The traces of the test, by visit"),
		@ApiResponse(responseCode = "404", description = "Unknown test id, or a test you are neither the owner of nor an admin for", content = @Content)
	})
	public ResponseEntity<List<Map<String, Object>>> listTraces(@Parameter(description = "Id of test") @PathVariable(name = "id") String testId) {

		if (!mayAccess(testId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<Map<String, Object>> traces = traceService.listTraces(testId).stream()
			.map(trace -> {
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("visit", trace.visit());
				entry.put("size", trace.size());
				return entry;
			})
			.toList();

		return new ResponseEntity<>(traces, HttpStatus.OK);
	}

	@GetMapping(value = "/log/{id}/traces/{visit}", produces = LogApi.APPLICATION_ZIP_VALUE)
	@Tag(name = SwaggerConfig.TAG_TEST_LOGS)
	@Operation(operationId = "downloadTestTrace", summary = "Download the Playwright browser trace of one scripted browser visit of a test",
		description = "The trace archive can be opened with 'npx playwright show-trace <file>' or at https://trace.playwright.dev/. "
			+ "Only recorded when the suite's scripted browser is Playwright with tracing enabled.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Trace archive",
			content = @Content(mediaType = LogApi.APPLICATION_ZIP_VALUE, schema = @Schema(type = "string", format = "binary"))),
		@ApiResponse(responseCode = "404", description = "Unknown test id, a test you are neither the owner of nor an admin for, "
			+ "or no trace was recorded for that visit", content = @Content)
	})
	public ResponseEntity<byte[]> downloadTrace(
		@Parameter(description = "Id of test") @PathVariable(name = "id") String testId,
		@Parameter(description = "Number of the scripted browser visit, as listed") @PathVariable(name = "visit") int visit) {

		if (!mayAccess(testId)) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Optional<byte[]> trace = traceService.getTrace(testId, visit);
		if (trace.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(LogApi.APPLICATION_ZIP_VALUE));
		headers.add("Content-Disposition", "attachment; filename=\"trace-" + testId + "-" + visit + ".zip\"");
		headers.setContentLength(trace.get().length);

		return new ResponseEntity<>(trace.get(), headers, HttpStatus.OK);
	}

	/**
	 * The same answer for an unknown test and for somebody else's, so that a test's existence does not leak.
	 */
	private boolean mayAccess(String testId) {
		ImmutableMap<String, String> testOwner = testInfoService.getTestOwner(testId);
		return testOwner != null
			&& (authenticationFacade.isAdmin() || authenticationFacade.getPrincipal().equals(testOwner));
	}
}
