package net.openid.conformance.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@Tag(name = SwaggerConfig.TAG_USER_AND_PREFERENCES)
@RequestMapping(value = "/api")
public class FavoritePlansApi {

	/**
	 * Longest accepted plan name. Real plan names are well under 100 characters; this only stops
	 * an authenticated client writing arbitrarily large strings into the favorites collection.
	 * Deliberately not a check against the live plan registry — favorites survive a plan being
	 * renamed or retired, and the picker already renders an unknown name as a removable
	 * "No longer available" row.
	 */
	static final int MAX_PLAN_NAME_LENGTH = 256;

	@Autowired
	private FavoritePlansService favoritePlansService;

	@GetMapping(value = "/favorite-plans", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "listFavoritePlans", summary = "Get the favorited test plans of the current user")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getFavoritePlans() {
		List<String> plans = favoritePlansService.getFavoritePlansForCurrentUser();
		return new ResponseEntity<>(wrapPlans(plans), HttpStatus.OK);
	}

	@PostMapping(value = "/favorite-plans",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "addFavoritePlan", summary = "Add a test plan to the current user's favorites")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Added successfully (idempotent)"),
		@ApiResponse(responseCode = "400",
			description = "Missing or invalid plan name, or the per-user favorites limit is reached")
	})
	public ResponseEntity<Object> addFavoritePlan(
			@Parameter(description = "An object containing the plan name to favorite, e.g. {\"plan\":\"planName\"}")
			@RequestBody JsonObject request) {

		JsonElement plan = request.get("plan");
		// isString rather than isJsonPrimitive: a JSON number or boolean is a primitive too, and
		// OIDFJSON.getString throws UnexpectedJsonTypeException on those, which surfaces as a 500
		// for what is plainly a malformed request.
		if (!OIDFJSON.isString(plan)) {
			return badRequest("A plan name is required to save a favorite.");
		}

		String planName = OIDFJSON.getString(plan);
		if (planName.isBlank()) {
			return badRequest("A plan name is required to save a favorite.");
		}
		if (planName.length() > MAX_PLAN_NAME_LENGTH) {
			return badRequest("That plan name is too long to save as a favorite.");
		}

		List<String> plans;
		try {
			plans = favoritePlansService.addFavoritePlanForCurrentUser(planName);
		} catch (FavoritePlansLimitExceededException e) {
			// A declined request, not a server fault. The message says what the user can do
			// about it — the picker shows it verbatim in the failure toast, where "please try
			// again" would send them round a loop that cannot succeed.
			return badRequest(e.getMessage());
		}
		return new ResponseEntity<>(wrapPlans(plans), HttpStatus.OK);
	}

	@DeleteMapping(value = "/favorite-plans/{planName}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "removeFavoritePlan", summary = "Remove a test plan from the current user's favorites")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Removed successfully (no-op if not favorited)")
	})
	public ResponseEntity<Object> removeFavoritePlan(
			@Parameter(description = "Name of the test plan to remove from favorites")
			@PathVariable String planName) {

		List<String> plans = favoritePlansService.removeFavoritePlanForCurrentUser(planName);
		return new ResponseEntity<>(wrapPlans(plans), HttpStatus.OK);
	}

	/**
	 * A 400 whose body carries the reason: {@code { "error": "<message>" }}. The picker surfaces
	 * the message directly in its failure toast, so it must read as something the user can act
	 * on rather than as an internal diagnostic.
	 */
	private ResponseEntity<Object> badRequest(String message) {
		JsonObject body = new JsonObject();
		body.addProperty("error", message);
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

	private JsonObject wrapPlans(List<String> plans) {
		JsonArray array = new JsonArray();
		for (String plan : plans) {
			array.add(plan);
		}
		JsonObject body = new JsonObject();
		body.add("plans", array);
		return body;
	}

}
