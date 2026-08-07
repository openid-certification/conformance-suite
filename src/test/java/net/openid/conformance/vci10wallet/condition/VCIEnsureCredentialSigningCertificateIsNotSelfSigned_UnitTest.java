package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureCredentialSigningCertificateIsNotSelfSigned_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureCredentialSigningCertificateIsNotSelfSigned cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialSigningCertificateIsNotSelfSigned();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		Security.addProvider(new BouncyCastleProvider());
	}

	private void putConfigWithSigningJwk(JsonObject signingJwk) {
		JsonObject credential = new JsonObject();
		credential.add("signing_jwk", signingJwk);
		JsonObject config = new JsonObject();
		config.add("credential", credential);
		env.putObject("config", config);
	}

	@Test
	public void failsWhenSigningJwkIsMissingFromConfig() {
		env.putObject("config", new JsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration"),
			"expected message to reference the missing 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void failsWhenSigningJwkCannotBeParsedAsJwk() {
		JsonObject notAJwk = new JsonObject();
		notAJwk.addProperty("kty", "not-a-real-kty");
		putConfigWithSigningJwk(notAJwk);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration"),
			"expected message to reference the unparseable 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void failsWhenSigningJwkHasNoCertificateChain() throws Exception {
		KeyPair keyPair = generateEcKeyPair();
		ECKey jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
		putConfigWithSigningJwk(JsonParser.parseString(jwk.toJSONString()).getAsJsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("must contain the signing certificate chain in its 'x5c' claim"),
			"expected message to reference the missing 'x5c' claim but was: " + err.getMessage());
	}

	@Test
	public void failsWhenFirstX5cEntryIsNotValidBase64() throws Exception {
		KeyPair keyPair = generateEcKeyPair();
		ECKey jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
		JsonObject jwkJson = JsonParser.parseString(jwk.toJSONString()).getAsJsonObject();
		JsonArray x5c = new JsonArray();
		x5c.add("%%%not-base64%%%");
		jwkJson.add("x5c", x5c);
		putConfigWithSigningJwk(jwkJson);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK'"),
			"expected message to reference the 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void failsWhenFirstX5cEntryIsNotACertificate() throws Exception {
		KeyPair keyPair = generateEcKeyPair();
		ECKey jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
		JsonObject jwkJson = JsonParser.parseString(jwk.toJSONString()).getAsJsonObject();
		JsonArray x5c = new JsonArray();
		x5c.add(java.util.Base64.getEncoder().encodeToString("this is not a certificate".getBytes()));
		jwkJson.add("x5c", x5c);
		putConfigWithSigningJwk(jwkJson);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK'"),
			"expected message to reference the 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void failsWhenSigningCertificateIsSelfSigned() throws Exception {
		KeyPair keyPair = generateEcKeyPair();
		X509Certificate selfSignedCert = signCert("CN=Unit Test", keyPair, "CN=Unit Test", keyPair);
		putConfigWithSigningJwk(jwkWithX5c(keyPair, selfSignedCert));

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK'") && err.getMessage().contains("self-signed"),
			"expected message to reference the 'Signing JWK' config field and self-signed cert but was: " + err.getMessage());
	}

	@Test
	public void passesWhenSigningCertificateIsNotSelfSigned() throws Exception {
		KeyPair leafKeyPair = generateEcKeyPair();
		KeyPair rootKeyPair = generateEcKeyPair();
		X509Certificate leafCert = signCert("CN=Leaf", leafKeyPair, "CN=Root", rootKeyPair);
		putConfigWithSigningJwk(jwkWithX5c(leafKeyPair, leafCert));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	private static JsonObject jwkWithX5c(KeyPair keyPair, X509Certificate cert) throws Exception {
		ECKey jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
			.x509CertChain(List.of(Base64.encode(cert.getEncoded())))
			.build();
		return JsonParser.parseString(jwk.toJSONString()).getAsJsonObject();
	}

	private static KeyPair generateEcKeyPair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(256);
		return kpg.generateKeyPair();
	}

	private static X509Certificate signCert(String subjectDN, KeyPair subjectKeyPair, String issuerDN, KeyPair issuerKeyPair) throws Exception {
		Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
		Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			new X500Name(issuerDN),
			BigInteger.valueOf(System.nanoTime()),
			notBefore, notAfter,
			new X500Name(subjectDN),
			subjectKeyPair.getPublic());
		ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC").build(issuerKeyPair.getPrivate());
		X509CertificateHolder holder = builder.build(signer);
		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
	}
}
