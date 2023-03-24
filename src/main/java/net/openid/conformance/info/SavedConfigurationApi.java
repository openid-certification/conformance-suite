package net.openid.conformance.info;

import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/api")
public class SavedConfigurationApi {

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@GetMapping(value = "/lastconfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get last configuration of current user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getLastConfig() {

		Document config = savedConfigurationService.getLastConfigForCurrentUser();

		if (config == null) {
			// always return a json object even if it's empty
			return new ResponseEntity<>(new JsonObject(), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(config, HttpStatus.OK);
		}

	}

}
