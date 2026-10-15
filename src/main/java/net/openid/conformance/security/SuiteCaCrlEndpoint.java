package net.openid.conformance.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.SuiteCaCrl;
import net.openid.conformance.util.TestKeysAndCerts;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves the certificate revocation lists of the suite's CAs, which the leaf certificates minted
 * under them name in their CRL distribution points. Public and reachable over plain http for the
 * reasons given on {@link MdocIacaCrlEndpoint}; the mdoc IACA's CRL is also served here, under
 * its slug, as well as at its own path.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class SuiteCaCrlEndpoint {

	/** The path prefix the CRLs are served under: {@code /crl/{slug}.crl}. */
	public static final String PATH_PREFIX = "/crl/";

	// no produces: a fetcher sending a narrow Accept header must still get the CRL, not a 406
	@GetMapping(PATH_PREFIX + "{slug}.crl")
	@ResponseBody
	@Operation(operationId = "suiteCaCrl",
		summary = "Returns the CRL of one of the suite's CA certificates",
		description = "Returns the DER encoded certificate revocation list issued by the named CA (served at"
			+ " /trust-anchors/{slug}.pem) for the leaf certificates the suite signs with. The list is"
			+ " empty: the suite revokes nothing.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The CRL, DER encoded",
			content = @Content(mediaType = MdocIacaCrl.CONTENT_TYPE, schema = @Schema(type = "string", format = "binary"))),
		@ApiResponse(responseCode = "404", description = "No CA with that name", content = @Content)
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<byte[]> getCrl(
			@Parameter(description = "The CA's name, as listed at /trust-anchors") @PathVariable("slug") String slug) {
		TestKeysAndCerts.SuiteCa ca = TrustAnchorEndpoint.caBySlug(slug);
		if (ca == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MdocIacaCrl.CONTENT_TYPE))
			.body(SuiteCaCrl.generate(ca));
	}
}
