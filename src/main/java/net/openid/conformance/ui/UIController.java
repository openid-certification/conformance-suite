package net.openid.conformance.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.export.LogEntryHelper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/ui")
class UIController {

	/**
	 * Returns the mapping for specification links.
	 *
	 * @return
	 */
	@GetMapping(value = "/spec_links", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get Spec Links")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public Map<String, String> getSpecLinks() {
		return LogEntryHelper.specLinks;
	}
}
