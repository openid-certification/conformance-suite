package net.openid.conformance.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Publish the public keys this server uses for signing.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class JwksEndpoint {

	@Autowired
	private KeyManager keyManager;

	@GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<JsonObject> getJwkSet() {
		JsonObject jwks = JsonParser.parseString(keyManager.getPublicKeys().toString()).getAsJsonObject(); // put it into a GSON object

		return new ResponseEntity<>(jwks, HttpStatus.OK);
	}

}
