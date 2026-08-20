package net.openid.conformance.info;

import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Tag(name = SwaggerConfig.TAG_USER_AND_PREFERENCES)
@RequestMapping(value = "/api")
public class SavedConfigurationApi {

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@GetMapping(value = "/lastconfig", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getLastConfig", summary = "Get last configuration of current user",
		description = "The most recently saved configuration document: owner, config, variant, time, and either 'testName' (saved when running a standalone test) or 'planName' (saved when creating a plan).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Retrieved successfully; an empty JSON object when the user has no saved configuration",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object")))
	})
	public ResponseEntity<Object> getLastConfig() {

		Document config = savedConfigurationService.getLastConfigForCurrentUser();

		if (config == null) {
			// always return a json object even if it's empty
			return new ResponseEntity<>(new JsonObject(), HttpStatus.OK);
		} else {
			// Wrap so the registered Gson serializer applies ConfigMigration to the nested
			// "config" object during the single response-body serialization pass — a user
			// whose last saved config used legacy vci.client_attestation_* keys will see
			// them surfaced under the new client_attestation.* shape, matching the runtime
			// fallbacks that the consumer-site conditions perform.
			return new ResponseEntity<>(new ConfigMigratingResponse(config), HttpStatus.OK);
		}

	}

}
