package net.openid.conformance.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.util.TestKeysAndCerts;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Publishes the suite's CA certificates ({@link TestKeysAndCerts.SuiteCa}): the trust anchors an
 * implementation under test pins, or a trusted-list operator lists, to accept what the suite
 * signs. {@code /trust-anchors} is a JSON index of them with the role each plays and the list a
 * trusted-list operator would put it in; {@code /trust-anchors/{slug}.pem} is one certificate,
 * served as text/plain so the PEM displays in a browser for copy/paste. The mdoc IACA root stays
 * at /mdoc-iaca-root.pem as well, since implementations have that path in their configuration.
 */
@Controller
@Tag(name = SwaggerConfig.TAG_SERVER)
public class TrustAnchorEndpoint {

	/** The path prefix the certificates are served under: {@code /trust-anchors/{slug}.pem}. */
	public static final String PATH_PREFIX = "/trust-anchors/";

	/** What each CA's leaves sign, and the ETSI TS 119 602 list of trusted entities it belongs in. */
	private record Role(String role, List<String> lists) {
	}

	private static final Map<TestKeysAndCerts.SuiteCa, Role> ROLES = Map.of(
		TestKeysAndCerts.SuiteCa.MDOC_IACA_ROOT, new Role(
			"Issues the document signer certificates in the mdoc credentials the suite creates (ISO/IEC 18013-5 IACA)"
				+ " and the MSO revocation list signer",
			List.of("PID providers", "mDL providers")),
		TestKeysAndCerts.SuiteCa.PID_PROVIDER, new Role(
			"Issues the certificate the suite signs SD-JWT VC credentials with",
			List.of("PID providers")),
		TestKeysAndCerts.SuiteCa.WALLET_PROVIDER, new Role(
			"Issues the certificate the suite signs wallet attestations and key attestations with",
			List.of("Wallet providers")),
		TestKeysAndCerts.SuiteCa.ACCESS, new Role(
			"Issues the suite's wallet-relying-party access certificate, which signs OpenID4VP authorization"
				+ " requests and issuer metadata and authenticates the suite as an mdoc reader",
			List.of("WRPAC providers")),
		TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER, new Role(
			"Issues the certificate the suite signs its wallet-relying-party registration certificate and that"
				+ " certificate's status list with",
			List.of("WRPRC providers"))
	);

	/** The name the sealing certificate of the suite's lists of trusted entities is served under. */
	public static final String LOTE_SIGNER_SLUG = "lote-signer";

	/** The CA served under {@code slug}, or null when there is none. */
	public static TestKeysAndCerts.SuiteCa caBySlug(String slug) {
		for (TestKeysAndCerts.SuiteCa ca : TestKeysAndCerts.SuiteCa.values()) {
			if (ca.getSlug().equals(slug)) {
				return ca;
			}
		}
		return null;
	}

	@GetMapping(value = "/trust-anchors", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(operationId = "trustAnchors",
		summary = "Lists the suite's CA certificates",
		description = "Lists the CA certificates that issue the certificates the suite signs with, with the URL"
			+ " each is served at, the URL of its CRL, what its leaf certificates sign and the ETSI TS 119 602"
			+ " list of trusted entities it belongs in.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The CA certificates",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object")))
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<String> listTrustAnchors() {
		JsonArray anchors = new JsonArray();
		for (TestKeysAndCerts.SuiteCa ca : TestKeysAndCerts.SuiteCa.values()) {
			Role role = ROLES.get(ca);
			JsonObject anchor = new JsonObject();
			anchor.addProperty("name", ca.getSlug());
			anchor.addProperty("subject", ca.getCert().getSubject().getName());
			anchor.addProperty("url", ca.getTrustAnchorUrl());
			anchor.addProperty("crl_url", ca.getCrlUrl());
			// an ephemeral CA stands in when the deployment lacks the published CA's key
			anchor.addProperty("ephemeral", ca.getIdentity().getEphemeral());
			anchor.addProperty("role", role.role());
			JsonArray lists = new JsonArray();
			role.lists().forEach(lists::add);
			anchor.add("lists", lists);
			anchors.add(anchor);
		}
		JsonObject body = new JsonObject();
		body.addProperty("organization", TestKeysAndCerts.ORGANIZATION_NAME);
		body.addProperty("organization_identifier", TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
		body.add("trust_anchors", anchors);
		JsonObject loteSigner = new JsonObject();
		loteSigner.addProperty("name", LOTE_SIGNER_SLUG);
		loteSigner.addProperty("subject", TestKeysAndCerts.getLoteSignerCert().getSubjectX500Principal().getName());
		loteSigner.addProperty("url", TestKeysAndCerts.getHttpsBaseUrl() + PATH_PREFIX + LOTE_SIGNER_SLUG + ".pem");
		loteSigner.addProperty("role", "Seals the suite's lists of trusted entities, served under /lote");
		loteSigner.addProperty("ephemeral", TestKeysAndCerts.getLoteSigner().getEphemeral());
		body.add("lote_signer", loteSigner);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body.toString());
	}

	@GetMapping(value = PATH_PREFIX + "{slug}.pem", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	@Operation(operationId = "trustAnchorCert",
		summary = "Returns one of the suite's CA certificates",
		description = "Returns the named CA certificate, PEM encoded; see /trust-anchors for the names.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "The CA certificate",
			content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", description = "A PEM encoded X.509 certificate"))),
		@ApiResponse(responseCode = "404", description = "No CA with that name", content = @Content)
	})
	// empty @SecurityRequirements clears the document-wide bearerAuth requirement: this endpoint is permitAll
	@SecurityRequirements
	public ResponseEntity<String> getTrustAnchorCert(
			@Parameter(description = "The CA's name, as listed at /trust-anchors") @PathVariable("slug") String slug) {
		if (LOTE_SIGNER_SLUG.equals(slug)) {
			return ResponseEntity.ok(TestKeysAndCerts.getLoteSignerCertPem() + "\n");
		}
		TestKeysAndCerts.SuiteCa ca = caBySlug(slug);
		if (ca == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ca.getCertPem() + "\n");
	}
}
