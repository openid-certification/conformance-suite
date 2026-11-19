package net.openid.conformance.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.export.LogEntryHelper;
import net.openid.conformance.security.LoginProviders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = SwaggerConfig.TAG_SERVER)
@RequestMapping(value = "/api/ui")
class UIController {

	private final LoginProviders loginProviders;

	UIController(LoginProviders loginProviders) {
		this.loginProviders = loginProviders;
	}

	/**
	 * Returns the mapping for specification links.
	 *
	 * @return
	 */
	@GetMapping(value = "/spec_links", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getSpecLinks", summary = "Get specification links",
		description = "Map of specification-reference prefix (as used in log entry 'requirements', e.g. 'OIDCC-') to the base URL of that specification.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", description = "Prefix-to-URL map")))
	})
	public Map<String, String> getSpecLinks() {
		return LogEntryHelper.specLinks;
	}

	@GetMapping(value = "/spec_section_links", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getSpecSectionLinks", summary = "Get specification section links",
		description = "Map of section requirement (e.g. 'BrazilOBDCR-7.1') to the full URL of that section, for specifications whose section fragments are not derivable from the section number. A requirement equal to a key, or starting with the key followed by '-', links to that URL in preference to the prefix map from /api/ui/spec_links.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", description = "Section-requirement-to-URL map")))
	})
	public Map<String, String> getSpecSectionLinks() {
		return LogEntryHelper.getSpecSectionLinks();
	}

	/**
	 * The login page offers a button for each of these. Public (with ?public=true)
	 * because it is read before anyone has signed in, so it carries each provider's
	 * registration id and display name and nothing else.
	 */
	@GetMapping(value = "/login_providers", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getLoginProviders", summary = "Get the configured login providers",
		description = "The login providers this server offers, in the order the login page shows them. 'id' is the registration id, so a login starts at /oauth2/authorization/{id}; 'name' is shown on the button.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "array", description = "List of {id, name}")))
	})
	public List<Map<String, String>> getLoginProviders() {
		return loginProviderSummaries(loginProviders);
	}

	static List<Map<String, String>> loginProviderSummaries(LoginProviders loginProviders) {
		return loginProviders.configured().stream()
			.map(provider -> Map.of("id", provider.id(), "name", provider.displayName()))
			.toList();
	}
}
