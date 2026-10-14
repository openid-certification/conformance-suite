package net.openid.conformance.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the /trust-anchors payloads: the index lists every suite CA with where to fetch it and
 * its CRL, and each PEM served is the CA that actually issues the suite's leaves. Pure unit test
 * by repo convention; anonymous reachability of the routes is covered by
 * scripts/run-security-tests.py.
 */
public class TrustAnchorEndpoint_UnitTest {

	private final TrustAnchorEndpoint endpoint = new TrustAnchorEndpoint();

	@Test
	public void indexListsEveryCaWithItsUrlsRoleAndLists() {
		ResponseEntity<String> response = endpoint.listTrustAnchors();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		JsonObject body = JsonParser.parseString(response.getBody()).getAsJsonObject();
		assertThat(OIDFJSON.getString(body.get("organization"))).isEqualTo(TestKeysAndCerts.ORGANIZATION_NAME);
		assertThat(OIDFJSON.getString(body.get("organization_identifier"))).isEqualTo(TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
		JsonArray anchors = body.getAsJsonArray("trust_anchors");
		assertThat(anchors).hasSize(TestKeysAndCerts.SuiteCa.values().length);
		for (var anchorEl : anchors) {
			JsonObject anchor = anchorEl.getAsJsonObject();
			TestKeysAndCerts.SuiteCa ca = TrustAnchorEndpoint.caBySlug(OIDFJSON.getString(anchor.get("name")));
			assertThat(ca).isNotNull();
			assertThat(OIDFJSON.getString(anchor.get("url"))).isEqualTo(ca.getTrustAnchorUrl());
			assertThat(OIDFJSON.getString(anchor.get("crl_url"))).isEqualTo(ca.getCrlUrl());
			assertThat(OIDFJSON.getString(anchor.get("subject"))).contains("OpenID Foundation");
			assertThat(OIDFJSON.getString(anchor.get("role"))).isNotBlank();
			assertThat(anchor.getAsJsonArray("lists")).isNotEmpty();
			assertThat(anchor.get("ephemeral").getAsJsonPrimitive().isBoolean()).isTrue();
		}
	}

	@ParameterizedTest
	@EnumSource(TestKeysAndCerts.SuiteCa.class)
	public void servesEachCaAsParsablePem(TestKeysAndCerts.SuiteCa ca) throws Exception {
		ResponseEntity<String> response = endpoint.getTrustAnchorCert(ca.getSlug());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).startsWith("-----BEGIN CERTIFICATE-----");
		assertThat(body).endsWith("-----END CERTIFICATE-----\n");

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		byte[] expected = ca.getCert().getEncoded().toByteArray(0, ca.getCert().getEncoded().getSize());
		assertThat(cert.getEncoded()).isEqualTo(expected);
	}

	@Test
	public void mdocIacaRootIsServedAtBothPaths() {
		assertThat(endpoint.getTrustAnchorCert("mdoc-iaca-root").getBody())
			.isEqualTo(new MdocIacaRootEndpoint().getMdocIacaRootCert().getBody());
	}

	@Test
	public void unknownNameIs404() {
		assertThat(endpoint.getTrustAnchorCert("no-such-ca").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(TrustAnchorEndpoint.caBySlug("no-such-ca")).isNull();
	}
}
