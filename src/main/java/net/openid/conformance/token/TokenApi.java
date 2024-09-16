package net.openid.conformance.token;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping(value = "/api")
public class TokenApi {

	@Autowired
	private TokenService tokenService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a list of existing tokens")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAllTokens() {

		return new ResponseEntity<>(tokenService.getAllTokens(), HttpStatus.OK);
	}

	@PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create new token")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Created token successfully"),
		@ApiResponse(responseCode = "403", description = "To create a token, you must not be an admin")
	})
	public ResponseEntity<Object> createToken(@Parameter(description = "For defining kind of token (permanent or temporary)") @RequestBody JsonObject request) {

		if (authenticationFacade.isAdmin()) {
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
	@Operation(summary = "Delete existing token by token Id")
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
