package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateKeyAttestationX5cCertificateChain_UnitTest {

	private static final String SELF_SIGNED_CERT = "MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg==";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateKeyAttestationX5cCertificateChain cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateKeyAttestationX5cCertificateChain();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		Security.addProvider(new BouncyCastleProvider());
	}

	private void putKeyAttestationJwt(String rawJwt, JsonArray x5c) {
		JsonObject header = new JsonObject();
		if (x5c != null) {
			header.add("x5c", x5c);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.addProperty("value", rawJwt);
		keyAttestationJwt.add("header", header);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	@Test
	public void skipsWhenX5cIsAbsent() {
		putKeyAttestationJwt("ignored.jwt.value", null);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void skipsWhenX5cIsEmpty() {
		putKeyAttestationJwt("ignored.jwt.value", new JsonArray());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenLeafIsSelfSigned() throws Exception {
		JsonArray x5c = new JsonArray();
		x5c.add(SELF_SIGNED_CERT);
		putKeyAttestationJwt("ignored.jwt.value", x5c);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void passesWhenChainIsValidAndSignatureMatchesLeaf() throws Exception {
		KeyPair leafKeyPair = generateEcKeyPair();
		KeyPair rootKeyPair = generateEcKeyPair();
		X509Certificate leafCert = signCert("CN=Leaf", leafKeyPair, "CN=Root", rootKeyPair);
		String jwt = signJwtWithEcPrivateKey(leafKeyPair);

		JsonArray x5c = new JsonArray();
		x5c.add(toBase64(leafCert));
		putKeyAttestationJwt(jwt, x5c);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenSignatureWasMadeWithDifferentKey() throws Exception {
		// Item 3 regression test: leaf cert public key A, JWT signed with key B.
		KeyPair leafKeyPair = generateEcKeyPair();
		KeyPair attackerKeyPair = generateEcKeyPair();
		KeyPair rootKeyPair = generateEcKeyPair();
		X509Certificate leafCert = signCert("CN=Leaf", leafKeyPair, "CN=Root", rootKeyPair);
		String jwt = signJwtWithEcPrivateKey(attackerKeyPair);

		JsonArray x5c = new JsonArray();
		x5c.add(toBase64(leafCert));
		putKeyAttestationJwt(jwt, x5c);

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
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

	private static String toBase64(X509Certificate cert) throws Exception {
		return java.util.Base64.getEncoder().encodeToString(cert.getEncoded());
	}

	private static String signJwtWithEcPrivateKey(KeyPair keyPair) throws Exception {
		ECKey jwk = new ECKey.Builder(Curve.P_256, (java.security.interfaces.ECPublicKey) keyPair.getPublic())
			.privateKey((ECPrivateKey) keyPair.getPrivate())
			.build();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issueTime(Date.from(Instant.now()))
			.build();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("key-attestation+jwt"))
			.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(new ECDSASigner(jwk));
		return jwt.serialize();
	}
}
