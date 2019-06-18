package io.fintechlabs.testframework.token;

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

import io.fintechlabs.testframework.security.AuthenticationFacade;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/api")
public class TokenApi {

	@Autowired
	private TokenService tokenService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getAllTokens() {

		return new ResponseEntity<>(tokenService.getAllTokens(), HttpStatus.OK);
	}

	@PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createToken(@RequestBody JsonObject request) {

		if (authenticationFacade.isAdmin())
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);

		JsonElement permanent = request.get("permanent");
		boolean isPermanent = false;
		try {
			if (permanent != null && permanent.isJsonPrimitive())
				isPermanent = permanent.getAsBoolean();
		} catch (ClassCastException e) {
			// Not a boolean
		}
		Object token = tokenService.createToken(isPermanent);
		return new ResponseEntity<>(token, HttpStatus.CREATED);
	}

	@DeleteMapping(value = "/token/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> deleteToken(@PathVariable("id") String id) {

		if (tokenService.deleteToken(id)) {
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
}
