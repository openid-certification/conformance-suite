package net.openid.conformance.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	@Operation(operationId = "getJwks", summary = "Get the public keys used to sign exported logs",
		description = "The JWKS published at this root /jwks endpoint contains only the keys the suite"
			+ " uses to sign downloaded/exported test logs and certification packages (the .sig files in"
			+ " the export zips) — it plays no part in any test protocol flow. Tests that need a"
			+ " protocol-level JWKS publish their own under the per-test /test/... URLs, and private-link"
			+ " share tokens are signed with a different, unpublished key. No authentication is required.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The JWK Set (public keys only)",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", description = "A JWK Set as defined by RFC 7517")))
	})
	@ResponseBody
	public ResponseEntity<JsonObject> getJwkSet() {
		JsonObject jwks = JsonParser.parseString(keyManager.getPublicKeys().toString()).getAsJsonObject(); // put it into a GSON object

		return new ResponseEntity<>(jwks, HttpStatus.OK);
	}

}
