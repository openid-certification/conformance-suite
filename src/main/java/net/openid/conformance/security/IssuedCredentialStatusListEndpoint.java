package net.openid.conformance.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.util.SuiteIssuedCredentialStatusList;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Serves the status list that credentials issued with the suite's built-in certificates reference
 * ({@link SuiteIssuedCredentialStatusList}), so their status can be checked after the test that
 * issued them has finished.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class IssuedCredentialStatusListEndpoint {

	// no produces: a fetcher sending a narrow Accept header must still get the list, not a 406
	@GetMapping(SuiteIssuedCredentialStatusList.PATH)
	@ResponseBody
	@Operation(operationId = "issuedCredentialStatusList",
		summary = "Returns the status list of the credentials the suite issues",
		description = "Returns the Token Status List that credentials issued with the suite's built-in certificates"
			+ " reference. The JWT form (the default) is signed by a certificate issued by the CA at"
			+ " /trust-anchors/pid-provider-ca.pem; the CWT form, returned when the Accept header asks for "
			+ StatusListCwt.CONTENT_TYPE + ", by a certificate issued by the CA at /trust-anchors/mdoc-iaca-root.pem.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The signed status list",
			content = {
				@Content(mediaType = JwtStatusListTokenClaimsBuilder.CONTENT_TYPE, schema = @Schema(type = "string", description = "A compact JWS")),
				@Content(mediaType = StatusListCwt.CONTENT_TYPE, schema = @Schema(type = "string", format = "binary", description = "A tagged COSE_Sign1"))
			})
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<byte[]> getStatusList(HttpServletRequest request) {
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		boolean cwt = accept != null && accept.toLowerCase(Locale.ROOT).contains(StatusListCwt.CONTENT_TYPE);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(cwt ? StatusListCwt.CONTENT_TYPE : JwtStatusListTokenClaimsBuilder.CONTENT_TYPE))
			.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
			.header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
			.body(cwt ? SuiteIssuedCredentialStatusList.cwt()
				: SuiteIssuedCredentialStatusList.jwt().getBytes(StandardCharsets.US_ASCII));
	}
}
