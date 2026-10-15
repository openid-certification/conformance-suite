package net.openid.conformance.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.util.SuiteWrprc;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.TimeUnit;

/**
 * Serves the Token Status List that the suite's registration certificate ({@link SuiteWrprc})
 * names in its {@code status}, so a wallet can check the certificate has not been revoked.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class WrprcStatusListEndpoint {

	@GetMapping(value = SuiteWrprc.STATUS_LIST_PATH, produces = SuiteWrprc.STATUS_LIST_CONTENT_TYPE)
	@ResponseBody
	@Operation(operationId = "wrprcStatusList",
		summary = "Returns the status list of the suite's registration certificate",
		description = "Returns the Token Status List (a compact JWS) that the suite's wallet-relying-party registration"
			+ " certificate points to. It is signed by a certificate issued by the CA at"
			+ " /trust-anchors/wrprc-provider-ca.pem.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The signed status list",
			content = @Content(mediaType = SuiteWrprc.STATUS_LIST_CONTENT_TYPE, schema = @Schema(type = "string", description = "A compact JWS")))
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<String> getStatusList() {
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(SuiteWrprc.STATUS_LIST_CONTENT_TYPE))
			.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
			.body(SuiteWrprc.statusListJwt());
	}
}
