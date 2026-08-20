package net.openid.conformance.token;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.apidoc.TokenCreatedResponse;
import net.openid.conformance.apidoc.TokenSummary;
import net.openid.conformance.security.AuthenticationFacade;
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

@Controller
@Tag(name = SwaggerConfig.TAG_API_TOKENS)
@RequestMapping(value = "/api")
public class TokenApi {

	@Autowired
	private TokenService tokenService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "listTokens", summary = "Get a list of existing tokens")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = TokenSummary.class))))
	})
	public ResponseEntity<Object> getAllTokens() {

		return new ResponseEntity<>(tokenService.getAllTokens(), HttpStatus.OK);
	}

	@PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "createToken", summary = "Create new token")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Created token successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenCreatedResponse.class))),
		@ApiResponse(responseCode = "403", description = "To create a token, you must not be an admin")
	})
	public ResponseEntity<Object> createToken(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Pass {\"permanent\": true} for a token that never expires; anything else (including omitting the field) creates a token valid for 24 hours",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject("{\"permanent\": true}")))
		@RequestBody JsonObject request) {

		if (authenticationFacade.isAdmin() || authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		JsonElement permanent = request.get("permanent");
		boolean isPermanent = false;
		try {
			if (permanent != null && permanent.isJsonPrimitive()) {
				isPermanent = OIDFJSON.getBoolean(permanent);
			}
		} catch (ClassCastException e) {
			// Not a boolean
		}
		Object token = tokenService.createToken(isPermanent);
		return new ResponseEntity<>(token, HttpStatus.CREATED);
	}

	@DeleteMapping(value = "/token/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "deleteToken", summary = "Delete existing token by token Id")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Deleted token successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find provided token Id")
	})
	public ResponseEntity<Object> deleteToken(@Parameter(description = "Id of token, use to identify a specific token") @PathVariable String id) {

		if (tokenService.deleteToken(id)) {
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
