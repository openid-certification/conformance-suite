package net.openid.conformance.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Hidden;
import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Admin-only API behind the server settings page. Hidden from the OpenAPI document: API tokens are
 * never admin, so the token-based access it documents cannot reach these endpoints; only an admin's
 * browser session can.
 */
@Controller
@Hidden
@RequestMapping(value = "/api/admin/settings")
public class ServerSettingsApi {

	private final ServerSettingsService serverSettingsService;

	private final AuthenticationFacade authenticationFacade;

	public ServerSettingsApi(ServerSettingsService serverSettingsService, AuthenticationFacade authenticationFacade) {
		this.serverSettingsService = serverSettingsService;
		this.authenticationFacade = authenticationFacade;
	}

	@GetMapping(value = "/cmf-chile", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getCMFChileSettings() {
		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		return ResponseEntity.ok(CMFChileSettingsView.toJson(serverSettingsService.getCMFChileSettings()));
	}

	// consumes JSON only, so a cross-site form post (which cannot set this content type without a
	// CORS preflight) is refused before it reaches the handler
	@PutMapping(value = "/cmf-chile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> putCMFChileSettings(@RequestBody JsonObject body) {
		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		CMFChileSettingsUpdate update;
		try {
			update = CMFChileSettingsUpdate.fromJson(body);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(errors(List.of(new SettingsError("", e.getMessage()))));
		}

		ServerSettingsService.SaveResult result =
			serverSettingsService.saveCMFChileSettings(update, authenticationFacade.getDisplayName());
		if (!result.errors().isEmpty()) {
			return ResponseEntity.badRequest().body(errors(result.errors()));
		}
		return ResponseEntity.ok(CMFChileSettingsView.toJson(result.settings()));
	}

	private static JsonObject errors(List<SettingsError> errors) {
		JsonArray array = new JsonArray();
		for (SettingsError error : errors) {
			JsonObject item = new JsonObject();
			item.addProperty("field", error.field());
			item.addProperty("message", error.message());
			array.add(item);
		}
		JsonObject body = new JsonObject();
		body.add("errors", array);
		return body;
	}
}
