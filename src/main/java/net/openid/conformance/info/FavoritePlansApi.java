package net.openid.conformance.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping(value = "/api")
public class FavoritePlansApi {

	@Autowired
	private FavoritePlansService favoritePlansService;

	@GetMapping(value = "/favorite-plans", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get the favorited test plans of the current user")
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
	@Operation(summary = "Add a test plan to the current user's favorites")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Added successfully (idempotent)"),
		@ApiResponse(responseCode = "400", description = "Missing or invalid plan name")
	})
	public ResponseEntity<Object> addFavoritePlan(
			@Parameter(description = "An object containing the plan name to favorite, e.g. {\"plan\":\"planName\"}")
			@RequestBody JsonObject request) {

		String planName = null;
		JsonElement plan = request.get("plan");
		if (plan != null && plan.isJsonPrimitive()) {
			planName = OIDFJSON.getString(plan);
		}

		if (planName == null || planName.isBlank()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		List<String> plans = favoritePlansService.addFavoritePlanForCurrentUser(planName);
		return new ResponseEntity<>(wrapPlans(plans), HttpStatus.OK);
	}

	@DeleteMapping(value = "/favorite-plans/{planName}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Remove a test plan from the current user's favorites")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Removed successfully (no-op if not favorited)")
	})
	public ResponseEntity<Object> removeFavoritePlan(
			@Parameter(description = "Name of the test plan to remove from favorites")
			@PathVariable String planName) {

		List<String> plans = favoritePlansService.removeFavoritePlanForCurrentUser(planName);
		return new ResponseEntity<>(wrapPlans(plans), HttpStatus.OK);
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
