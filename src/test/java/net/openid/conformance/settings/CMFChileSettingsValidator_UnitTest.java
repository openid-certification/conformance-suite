package net.openid.conformance.settings;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CMFChileSettingsValidator_UnitTest {

	private static CMFChileSettings valid;
	private static KeyPair rsaKeys;
	private static String rsaCertificatePem;
	private static KeyPair otherRsaKeys;
	private static RSAKey jwk;

	@BeforeAll
	public static void createMaterial() throws Exception {
		valid = TestPki.validSettings();
		rsaKeys = TestPki.rsaKeyPair();
		rsaCertificatePem = TestPki.pem(TestPki.selfSigned(rsaKeys, "rsa"));
		otherRsaKeys = TestPki.rsaKeyPair();
		jwk = TestPki.rsaJwk("sig-1");
	}

	private static CMFChileSettings withEndpoint(String endpoint) {
		return new CMFChileSettings(endpoint, valid.clientId(), valid.clientSecret(), valid.clientJwks(),
			valid.positiveCertificates(), valid.negativeCertificates(), null, null);
	}

	private static CMFChileSettings withJwks(String jwks) {
		return new CMFChileSettings(valid.directoryTokenEndpoint(), valid.clientId(), valid.clientSecret(), jwks,
			valid.positiveCertificates(), valid.negativeCertificates(), null, null);
	}

	private static CMFChileSettings withPositive(CertificateEntry... entries) {
		return new CMFChileSettings(valid.directoryTokenEndpoint(), valid.clientId(), valid.clientSecret(),
			valid.clientJwks(), List.of(entries), valid.negativeCertificates(), null, null);
	}

	private static List<String> fields(List<SettingsError> errors) {
		return errors.stream().map(SettingsError::field).toList();
	}

	@Test
	public void acceptsAFullyPopulatedSectionIncludingAnExpiredNegativeCertificate() {
		assertThat(CMFChileSettingsValidator.validate(valid)).isEmpty();
	}

	@Test
	public void acceptsAnEmptySection() {
		assertThat(CMFChileSettingsValidator.validate(CMFChileSettings.empty())).isEmpty();
	}

	@Test
	public void rejectsAnHttpTokenEndpoint() {
		List<SettingsError> errors = CMFChileSettingsValidator.validate(withEndpoint("http://directory.example.cl/token"));

		assertThat(errors).containsExactly(new SettingsError("directoryTokenEndpoint",
			"'Directory token endpoint URL' must be an absolute https:// URL"));
	}

	@Test
	public void rejectsARelativeTokenEndpoint() {
		assertThat(fields(CMFChileSettingsValidator.validate(withEndpoint("/token")))).containsExactly("directoryTokenEndpoint");
	}

	@Test
	public void rejectsAnUnparseableTokenEndpoint() {
		assertThat(fields(CMFChileSettingsValidator.validate(withEndpoint("https://exa mple.cl")))).containsExactly("directoryTokenEndpoint");
	}

	@Test
	public void rejectsAJwksThatDoesNotParse() {
		List<SettingsError> errors = CMFChileSettingsValidator.validate(withJwks("{\"keys\": 3}"));

		assertThat(fields(errors)).containsExactly("clientJwks");
		assertThat(errors.get(0).message()).startsWith("'Client JWKS' is not a valid JWKS");
	}

	@Test
	public void rejectsAnEmptyJwks() {
		List<SettingsError> errors = CMFChileSettingsValidator.validate(withJwks("{\"keys\": []}"));

		assertThat(errors).containsExactly(new SettingsError("clientJwks", "'Client JWKS' contains no keys"));
	}

	@Test
	public void rejectsAPublicOnlyKey() {
		List<SettingsError> errors = CMFChileSettingsValidator.validate(withJwks(new JWKSet(jwk).toString(true)));

		assertThat(fields(errors)).containsExactly("clientJwks");
		assertThat(errors.get(0).message()).contains("'sig-1'").contains("no private key");
	}

	@Test
	public void rejectsAKeyWithoutKid() throws Exception {
		RSAKey noKid = new RSAKey.Builder(jwk).keyID(null).build();

		List<SettingsError> errors = CMFChileSettingsValidator.validate(withJwks(new JWKSet(noKid).toString(false)));

		assertThat(errors).containsExactly(new SettingsError("clientJwks", "'Client JWKS' key 1 has no 'kid'"));
	}

	@Test
	public void rejectsAnEntryWithoutLabel() throws Exception {
		CertificateEntry entry = new CertificateEntry("x", " ", rsaCertificatePem, TestPki.pkcs8Pem(rsaKeys.getPrivate()));

		List<SettingsError> errors = CMFChileSettingsValidator.validate(withPositive(entry));

		assertThat(errors).containsExactly(new SettingsError("positiveCertificates[0].label",
			"'Label' is required for entry 1 in 'Positive DCR client certificates'"));
	}

	@Test
	public void rejectsADuplicateLabelWithinAList() throws Exception {
		String key = TestPki.pkcs8Pem(rsaKeys.getPrivate());
		CertificateEntry first = new CertificateEntry("a", "primary", rsaCertificatePem, key);
		CertificateEntry second = new CertificateEntry("b", "primary", rsaCertificatePem, key);

		assertThat(fields(CMFChileSettingsValidator.validate(withPositive(first, second))))
			.containsExactly("positiveCertificates[1].label");
	}

	@Test
	public void theSameLabelInBothListsIsAllowed() throws Exception {
		CertificateEntry entry = new CertificateEntry("a", "expired", rsaCertificatePem, TestPki.pkcs8Pem(rsaKeys.getPrivate()));

		assertThat(CMFChileSettingsValidator.validate(withPositive(entry))).isEmpty();
	}

	@Test
	public void rejectsABadCertificateChain() throws Exception {
		CertificateEntry entry = new CertificateEntry("a", "primary", "garbage", TestPki.pkcs8Pem(rsaKeys.getPrivate()));

		List<SettingsError> errors = CMFChileSettingsValidator.validate(withPositive(entry));

		assertThat(errors).containsExactly(new SettingsError("positiveCertificates[0].certificateChainPem",
			"'Certificate chain' of 'primary' in 'Positive DCR client certificates' is invalid: no certificate found"));
	}

	@Test
	public void rejectsAMissingPrivateKey() {
		CertificateEntry entry = new CertificateEntry("a", "primary", rsaCertificatePem, null);

		List<SettingsError> errors = CMFChileSettingsValidator.validate(withPositive(entry));

		assertThat(errors).containsExactly(new SettingsError("positiveCertificates[0].privateKeyPem",
			"'Private key' is required for 'primary' in 'Positive DCR client certificates'"));
	}

	@Test
	public void rejectsAnUnparseablePrivateKey() {
		CertificateEntry entry = new CertificateEntry("a", "primary", rsaCertificatePem, "garbage");

		assertThat(fields(CMFChileSettingsValidator.validate(withPositive(entry))))
			.containsExactly("positiveCertificates[0].privateKeyPem");
	}

	@Test
	public void rejectsAKeyThatDoesNotMatchTheCertificate() throws Exception {
		CertificateEntry entry = new CertificateEntry("a", "primary", rsaCertificatePem, TestPki.pkcs8Pem(otherRsaKeys.getPrivate()));

		List<SettingsError> errors = CMFChileSettingsValidator.validate(withPositive(entry));

		assertThat(errors).containsExactly(new SettingsError("positiveCertificates[0].privateKeyPem",
			"'Private key' of 'primary' in 'Positive DCR client certificates' does not match the first certificate in its chain"));
	}

	@Test
	public void acceptsAPkcs1RsaKey() throws Exception {
		CertificateEntry entry = new CertificateEntry("a", "primary", rsaCertificatePem, TestPki.pem(rsaKeys.getPrivate()));

		assertThat(CMFChileSettingsValidator.validate(withPositive(entry))).isEmpty();
	}

	@Test
	public void reportsNegativeListErrorsUnderTheirOwnField() {
		CMFChileSettings settings = new CMFChileSettings(null, null, null, null, List.of(),
			List.of(new CertificateEntry("n", "expired", rsaCertificatePem, null)), null, null);

		List<SettingsError> errors = CMFChileSettingsValidator.validate(settings);

		assertThat(fields(errors)).containsExactly("negativeCertificates[0].privateKeyPem");
		assertThat(errors.get(0).message()).contains("'Negative DCR client certificates'");
	}
}
