package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The suite-wide status list of credentials the suite issues is signed by the issuer of those
 * credentials and says the same as the list each test instance serves.
 */
public class SuiteIssuedCredentialStatusList_UnitTest {

	private static final Instant ISSUED = Instant.parse("2026-10-28T10:00:00Z");

	@Test
	public void theJwtIsSignedByThePidProviderLeafAndHasTheEvenOddContents() throws Exception {
		JWSObject jws = JWSObject.parse(SuiteIssuedCredentialStatusList.jwt(ISSUED));
		assertThat(jws.getHeader().getType().toString()).isEqualTo("statuslist+jwt");
		assertThat(jws.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
		X509Certificate leaf = X509CertUtils.parse(jws.getHeader().getX509CertChain().get(0).decode());
		assertThat(jws.verify(new ECDSAVerifier((ECPublicKey) leaf.getPublicKey()))).isTrue();
		leaf.verify(pidProviderCaCert().getPublicKey());
		assertThat(leaf.getPublicKey()).isEqualTo(TestKeysAndCerts.getSdJwtPidSigner().getJwk().toECPublicKey());

		JsonObject payload = JsonParser.parseString(jws.getPayload().toString()).getAsJsonObject();
		assertThat(OIDFJSON.getString(payload.get("sub"))).isEqualTo(SuiteIssuedCredentialStatusList.url());
		assertThat(OIDFJSON.getLong(payload.get("exp")) - OIDFJSON.getLong(payload.get("iat"))).isEqualTo(24 * 3600);
		JsonObject statusList = payload.getAsJsonObject("status_list");
		TokenStatusList list = TokenStatusList.decode(OIDFJSON.getString(statusList.get("lst")), OIDFJSON.getInt(statusList.get("bits")));
		for (int i = 0; i < EvenOddStatusListContents.STATUS_LIST_ENTRIES; i++) {
			assertThat(list.getStatus(i)).isEqualTo(i % 2 == 0 ? TokenStatusList.Status.VALID : TokenStatusList.Status.INVALID);
		}
	}

	@Test
	public void theCwtIsATaggedCoseSign1NamingTheSameUrl() {
		byte[] cwt = SuiteIssuedCredentialStatusList.cwt(ISSUED);
		// CBOR tag 18 (COSE_Sign1)
		assertThat(cwt[0] & 0xff).isEqualTo(0xd2);
		assertThat(new String(cwt, StandardCharsets.ISO_8859_1)).contains(SuiteIssuedCredentialStatusList.url());
	}

	@Test
	public void theUrlIsUnderTheSuitesBaseUrl() {
		assertThat(SuiteIssuedCredentialStatusList.url())
			.isEqualTo(TestKeysAndCerts.getHttpsBaseUrl() + "/status-lists/issued-credentials");
	}

	private static X509Certificate pidProviderCaCert() throws Exception {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
			new ByteArrayInputStream(TestKeysAndCerts.SuiteCa.PID_PROVIDER.getCertPem().getBytes(StandardCharsets.US_ASCII)));
	}
}
