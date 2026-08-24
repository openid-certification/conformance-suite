package net.openid.conformance.security;

import io.swagger.v3.oas.annotations.Operation;
import net.openid.conformance.util.TestKeysAndCerts;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Publishes the IACA root certificate that issues the document signer certificates the suite
 * embeds in the mdoc credentials it creates (as the emulated wallet in OpenID4VP verifier
 * tests and as the emulated issuer in OpenID4VCI wallet tests). Implementations under test
 * should configure this certificate as a trust anchor.
 *
 * Served as text/plain so the PEM displays in a browser for copy/paste.
 */
@Controller
public class MdocIacaRootEndpoint {

	@GetMapping(value = "/mdoc-iaca-root.pem", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	@Operation(operationId = "mdocIacaRootCert",
		summary = "Returns the certificate used for mdoc signing and verification",
		description = "Returns the IACA root certificate used for mdoc signing and verification")
	public ResponseEntity<String> getMdocIacaRootCert() {
		return ResponseEntity.ok(TestKeysAndCerts.IACA_ROOT_CERT_PEM + "\n");
	}
}
