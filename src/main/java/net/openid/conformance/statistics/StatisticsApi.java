package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.statistics.AsyncSnapshotCache.Failed;
import net.openid.conformance.statistics.AsyncSnapshotCache.Failure;
import net.openid.conformance.statistics.AsyncSnapshotCache.Pending;
import net.openid.conformance.statistics.AsyncSnapshotCache.Ready;
import net.openid.conformance.statistics.AsyncSnapshotCache.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "/api")
public class StatisticsApi {

	/** How long the client should wait before polling again while the first snapshot computes. */
	private static final String RETRY_AFTER_SECONDS = "2";

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/statistics/overview", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get suite-wide usage statistics across all users (admin only)",
		description = "Served from a snapshot that is recomputed in the background at most every 12 hours, "
			+ "because the underlying aggregations group over the whole test database. "
			+ "While the first snapshot is being computed the response is 202 and the client should poll.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "A snapshot is available",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Ready.class))),
		@ApiResponse(responseCode = "202", description = "The first snapshot is still being computed; retry after the Retry-After interval",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Pending.class))),
		// empty @Content = no body schema; without it springdoc infers Object from the return type
		@ApiResponse(responseCode = "403", description = "You must be an admin to view statistics", content = @Content),
		@ApiResponse(responseCode = "500", description = "There is no snapshot to serve and computing one failed",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Failed.class)))
	})
	// The return type is deliberately left unparameterised. Gson serialises a body by its
	// RUNTIME class only while the declared type is not a ParameterizedType (see
	// GsonHttpMessageConverter.writeInternal), and the sealed StatisticsResponse itself
	// declares no fields - so anything that gets Gson to serialise AGAINST the declared
	// type instead (a parameterised wrapper, or a direct Gson.toJson(body, type)) turns
	// every response into "{}".
	public ResponseEntity<Object> getOverview(
		@Parameter(description = "Recompute the snapshot even if the current one is still fresh; any existing snapshot keeps being served meanwhile")
		@RequestParam(defaultValue = "false") boolean refresh) {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		State<StatisticsOverview> state = statisticsService.getOverview(refresh);
		return switch (state) {
			case Ready<StatisticsOverview> ready -> new ResponseEntity<>(
				new StatisticsResponse.Ready("ready", ready.computedAt().toString(), ready.computeDuration().toMillis(),
					ready.refreshing(), lastError(ready.lastFailure()), ready.value()),
				HttpStatus.OK);
			case Pending<StatisticsOverview> pending -> ResponseEntity.status(HttpStatus.ACCEPTED)
				.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
				.body(new StatisticsResponse.Pending("pending", pending.startedAt().toString()));
			case Failed<StatisticsOverview> failed -> new ResponseEntity<>(
				new StatisticsResponse.Failed("error", failed.failure().message(), failed.failure().failedAt().toString()),
				HttpStatus.INTERNAL_SERVER_ERROR);
		};
	}

	private static StatisticsResponse.LastError lastError(Failure failure) {
		if (failure == null) {
			return null;
		}
		return new StatisticsResponse.LastError(failure.message(), failure.failedAt().toString());
	}
}
