package net.openid.conformance.statistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.statistics.AsyncSnapshotCache.Failed;
import net.openid.conformance.statistics.AsyncSnapshotCache.Failure;
import net.openid.conformance.statistics.AsyncSnapshotCache.Pending;
import net.openid.conformance.statistics.AsyncSnapshotCache.Ready;
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
	@Operation(operationId = "getStatisticsOverview",
		summary = "Get suite-wide usage statistics across all users (admin only)",
		description = "Served from a snapshot that is recomputed in the background at most every 12 hours, "
			+ "because the underlying aggregations group over the whole test database, and dropped after "
			+ "12 hours without a request, so that it only takes memory while somebody is looking. "
			+ "While a snapshot is being computed and none is held the response is 202 and the client should poll. "
			+ "The filters and the range are applied to that snapshot when the request is answered, so they "
			+ "are free to change and never trigger a recomputation.\n\n"
			+ "In addition to the parameters below, any number of plan level variant filters may be sent as "
			+ "`variant.<parameter>=<value>`, e.g. `variant.fapi_profile=openbanking_brazil&variant.client_auth_type=mtls`; "
			+ "a cell has to match all of them. They cannot be declared individually here because the "
			+ "parameter names are the variant parameters of every test plan the suite publishes.\n\n"
			+ "`data.modules` covers the trailing 12 months only, is clipped to the range by month whatever "
			+ "the granularity, and honours `family` and `plan` as registry membership - a module belongs to "
			+ "every family that has a plan running it. The `variant.<parameter>` and `cert` filters do not "
			+ "apply to it: a test run records neither in a form the module counts can be keyed by. Its `runs` "
			+ "counts only runs by an identified user, since the section counts people and a run written before "
			+ "authentication completed belongs to nobody, so it does not reconcile exactly with the runs charts.\n\n"
			+ "`data.heatmap` and `data.externalHosts` cover the trailing 12 months, and the summary tiles are "
			+ "windowed too: `inProgress` and `stuck` count runs started since the server came up (a run left "
			+ "RUNNING or WAITING by a restart is not in progress), `totalTests` is the run "
			+ "collection's own document count (an estimate to within a few documents) and `totalUsers` counts "
			+ "users who created a test plan, so somebody who has only ever run standalone tests is not in it. "
			+ "Everything else is all time.")
	@Parameters({
		@Parameter(name = "granularity", in = ParameterIn.QUERY,
			description = "The time buckets to report in. Monthly covers the whole history; weekly covers the "
				+ "trailing 104 weeks and is keyed by the Monday of each ISO week.",
			schema = @Schema(type = "string", allowableValues = {"month", "week"}, defaultValue = "month")),
		@Parameter(name = "from", in = ParameterIn.QUERY,
			description = "The first period to report, inclusive; `YYYY-MM` for monthly, `YYYY-MM-DD` for weekly "
				+ "(any day of the week, snapped to its Monday). Defaults to as far back as there is data.",
			schema = @Schema(type = "string", example = "2026-06")),
		@Parameter(name = "to", in = ParameterIn.QUERY,
			description = "The last period to report, inclusive, in the same format as `from`. Defaults to today.",
			schema = @Schema(type = "string", example = "2026-08")),
		@Parameter(name = "family", in = ParameterIn.QUERY,
			description = "Only count test plans of this spec family, as named in the `families` list of the response.",
			schema = @Schema(type = "string", example = "FAPI-CIBA")),
		@Parameter(name = "plan", in = ParameterIn.QUERY,
			description = "Only count this test plan, by name, as listed in `dimensions.plans`.",
			schema = @Schema(type = "string", example = "fapi-ciba-id1-test-plan")),
		@Parameter(name = "cert", in = ParameterIn.QUERY,
			description = "Only count test plans certified for this profile, as listed in "
				+ "`dimensions.certProfiles`; a plan certified for several profiles matches any one of them.",
			schema = @Schema(type = "string"))
	})
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "A snapshot is available",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Ready.class))),
		@ApiResponse(responseCode = "202", description = "The first snapshot is still being computed; retry after the Retry-After interval",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Pending.class))),
		@ApiResponse(responseCode = "400", description = "A filter or range parameter could not be used",
			content = @Content(schema = @Schema(implementation = StatisticsResponse.Invalid.class))),
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
		@RequestParam(defaultValue = "false") boolean refresh,
		// the filters are read from the raw parameter map because variant.<parameter> is
		// an open set of names that no @RequestParam can declare
		@Parameter(hidden = true) HttpServletRequest request) {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		StatisticsQuery query;
		try {
			query = StatisticsQuery.parse(request.getParameterMap());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(new StatisticsResponse.Invalid(e.getMessage()));
		}

		// 200 whenever a snapshot exists, 202 while the first one is being computed, 500
		// when there is none and computing one failed
		return switch (statisticsService.getCube(refresh)) {
			case Ready<StatisticsCube> ready -> new ResponseEntity<>(new StatisticsResponse.Ready(
				ready.computedAt().toString(), ready.computeDuration().toMillis(), ready.refreshing(),
				lastError(ready.lastFailure()), StatisticsSlicer.slice(ready.value(), query)), HttpStatus.OK);
			case Pending<StatisticsCube> pending -> ResponseEntity.status(HttpStatus.ACCEPTED)
				.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
				.body(new StatisticsResponse.Pending(pending.startedAt().toString()));
			case Failed<StatisticsCube> failed -> new ResponseEntity<>(
				new StatisticsResponse.Failed(failed.failure().message(), failed.failure().failedAt().toString()),
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
