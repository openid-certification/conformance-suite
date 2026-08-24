package net.openid.conformance.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.export.LogEntryHelper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = SwaggerConfig.TAG_SERVER)
@RequestMapping(value = "/api/ui")
class UIController {

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
}
