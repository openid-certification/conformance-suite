package net.openid.conformance.token;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/api")
public class TokenApi {

	@Autowired
	private TokenService tokenService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get a list of existing tokens")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAllTokens() {

		return new ResponseEntity<>(tokenService.getAllTokens(), HttpStatus.OK);
	}

	@PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create new token")
	@ApiResponses({
		@ApiResponse(code = 201, message = "Created token successfully"),
		@ApiResponse(code = 403, message = "In order to create token, You must be an admin")
	})
	public ResponseEntity<Object> createToken(@ApiParam(value = "For defining kind of token (permanent or temporary)") @RequestBody JsonObject request) {

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
	@ApiOperation(value = "Delete existing token by token Id")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Deleted token successfully"),
		@ApiResponse(code = 404, message = "Couldn't find provided token Id")
	})
	public ResponseEntity<Object> deleteToken(@ApiParam(value = "Id of token, use to identify a specific token") @PathVariable("id") String id) {

		if (tokenService.deleteToken(id)) {
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
