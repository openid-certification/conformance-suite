package net.openid.conformance.settings;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CMFChileSettingsParser_UnitTest {

	private static CMFChileSettings stored;

	@BeforeAll
	public static void createSettings() throws Exception {
		stored = TestPki.validSettings();
	}

	@Test
	public void parsesTheWholeSection() {
		CMFChileDirectorySettings parsed = CMFChileSettingsParser.parse(stored);

		assertThat(parsed.directoryTokenEndpoint()).isEqualTo("https://directory.example.cl/token");
		assertThat(parsed.clientId()).isEqualTo("oidf-conformance");
		assertThat(parsed.clientSecret()).isEqualTo("the-client-secret");
		assertThat(parsed.clientJwks().getKeyByKeyId("sig-1").isPrivate()).isTrue();
	}

	@Test
	public void parsesCertificatesAndKeysIntoJavaTypes() {
		CMFChileDirectorySettings parsed = CMFChileSettingsParser.parse(stored);

		ClientCertificate positive = parsed.positiveCertificates().get(0);
		assertThat(positive.label()).isEqualTo("primary");
		assertThat(positive.leaf().getSubjectX500Principal().getName()).isEqualTo("CN=positive");
		assertThat(positive.privateKey().getAlgorithm()).isEqualTo("RSA");
		assertThat(PemParsing.keyMatchesCertificate(positive.privateKey(), positive.leaf())).isTrue();

		ClientCertificate negative = parsed.negativeCertificates().get(0);
		assertThat(negative.label()).isEqualTo("expired");
		assertThat(negative.privateKey().getAlgorithm()).isIn("EC", "ECDSA");
		assertThat(PemParsing.keyMatchesCertificate(negative.privateKey(), negative.leaf())).isTrue();
	}

	@Test
	public void keepsTheChainInOrder() throws Exception {
		var leafKeys = TestPki.rsaKeyPair();
		var issuerKeys = TestPki.rsaKeyPair();
		var issuer = TestPki.selfSigned(issuerKeys, "issuer");
		var leaf = TestPki.certificate(leafKeys, "leaf", issuerKeys, "issuer",
			java.time.Instant.now().minusSeconds(60), java.time.Instant.now().plusSeconds(3600));
		CMFChileSettings settings = new CMFChileSettings(null, null, null, null,
			List.of(new CertificateEntry("a", "chain", TestPki.pem(leaf, issuer), TestPki.pkcs8Pem(leafKeys.getPrivate()))),
			List.of(), null, null);

		ClientCertificate parsed = CMFChileSettingsParser.parse(settings).positiveCertificates().get(0);

		assertThat(parsed.certificateChain()).containsExactly(leaf, issuer);
		assertThat(parsed.leaf()).isEqualTo(leaf);
	}

	@Test
	public void unsetFieldsAreNullOrEmpty() {
		CMFChileDirectorySettings parsed = CMFChileSettingsParser.parse(CMFChileSettings.empty());

		assertThat(parsed.directoryTokenEndpoint()).isNull();
		assertThat(parsed.clientSecret()).isNull();
		assertThat(parsed.clientJwks()).isNull();
		assertThat(parsed.positiveCertificates()).isEmpty();
		assertThat(parsed.negativeCertificates()).isEmpty();
	}

	@Test
	public void aCorruptCertificateThrowsNamingTheField() {
		CMFChileSettings corrupt = new CMFChileSettings(null, null, null, null, List.of(),
			List.of(stored.negativeCertificates().get(0), new CertificateEntry("x", "broken", "garbage", "garbage")),
			null, null);

		assertThatThrownBy(() -> CMFChileSettingsParser.parse(corrupt))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("negativeCertificates[1]")
			.hasMessageContaining("broken");
	}

	@Test
	public void aCorruptJwksThrowsNamingTheField() {
		CMFChileSettings corrupt = new CMFChileSettings(null, null, null, "not json", List.of(), List.of(), null, null);

		assertThatThrownBy(() -> CMFChileSettingsParser.parse(corrupt))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("clientJwks");
	}

	@Test
	public void toStringOmitsSecretsAndKeys() {
		CMFChileDirectorySettings parsed = CMFChileSettingsParser.parse(stored);

		assertThat(parsed.toString())
			.doesNotContain("the-client-secret")
			.doesNotContain("\"d\"")
			.contains("oidf-conformance")
			.contains("CN=positive");
		assertThat(parsed.positiveCertificates().get(0).toString()).doesNotContain("PRIVATE").contains("primary");
	}
}
