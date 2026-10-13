package net.openid.conformance.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.util.MdocIacaCrl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves the certificate revocation list of the suite's mdoc IACA (the root published at
 * /mdoc-iaca-root.pem), which the document signer certificates minted under it name in their
 * CRL distribution point. A verifier or wallet under test that checks document signer
 * revocation fetches it from here, so it is public, and it is also reachable over plain http:
 * CRL distribution points are http URLs by PKI convention (RFC 5280 only describes HTTP, FTP
 * and LDAP, the CA/Browser Forum requires HTTP, and Windows refuses https), and the CRL needs
 * no transport security because the IACA's signature authenticates it. See
 * {@link RejectPlainHttpTrafficFilter}.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class MdocIacaCrlEndpoint {

	// no produces: a fetcher sending a narrow Accept header must still get the CRL, not a 406
	@GetMapping(MdocIacaCrl.PATH)
	@ResponseBody
	@Operation(operationId = "mdocIacaCrl",
		summary = "Returns the CRL of the mdoc IACA root certificate",
		description = "Returns the DER encoded certificate revocation list issued by the mdoc IACA root"
			+ " certificate (served at /mdoc-iaca-root.pem) for the document signer certificates the suite"
			+ " embeds in the mdoc credentials it creates. The list is empty: the suite revokes nothing.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The CRL, DER encoded",
			content = @Content(mediaType = MdocIacaCrl.CONTENT_TYPE, schema = @Schema(type = "string", format = "binary")))
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<byte[]> getMdocIacaCrl() {
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MdocIacaCrl.CONTENT_TYPE))
			.body(MdocIacaCrl.generate());
	}
}
