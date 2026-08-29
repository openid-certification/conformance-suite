package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.RicalTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateRequestObjectSignerReaderAuthCertificateProfile_UnitTest {

	private ValidateRequestObjectSignerReaderAuthCertificateProfile cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRequestObjectSignerReaderAuthCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesForAProfileConformantCertificate() throws Exception {
		putRequestObjectSignedWithProfileConformantCert();

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsForACertificateWithoutTheReaderAuthEku() {
		// the fixture reader certificate has digitalSignature key usage but no EKU and no CRL
		// distribution points
		RicalTestFixtures.putSignedRequestObject(env, RicalTestFixtures.generateReaderPki());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("mdlReaderAuth"), e.getMessage());
		assertTrue(e.getMessage().contains("CRL distribution points"), e.getMessage());
	}

	private void putRequestObjectSignedWithProfileConformantCert() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();

		X500Name name = new X500Name("C=UT,CN=Conformant Reader");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			new BigInteger(80, new SecureRandom()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(key.toECPublicKey().getEncoded()));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
		builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(
			new KeyPurposeId[] {
				KeyPurposeId.getInstance(new org.bouncycastle.asn1.ASN1ObjectIdentifier(
					ValidateRequestObjectSignerReaderAuthCertificateProfile.MDL_READER_AUTH_EKU_OID)),
			}));
		builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(
			new DistributionPoint[] { new DistributionPoint(
				new DistributionPointName(new GeneralNames(
					new GeneralName(GeneralName.uniformResourceIdentifier, "http://example.com/test.crl"))),
				null, null) }));
		byte[] der = builder
			.build(new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey()))
			.getEncoded();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256)
				.x509CertChain(List.of(com.nimbusds.jose.util.Base64.encode(der)))
				.build(),
			new JWTClaimsSet.Builder().claim("client_id", "x509_san_dns:verifier.example.com").build());
		jwt.sign(new ECDSASigner(key));

		JsonObject requestObject = new JsonObject();
		requestObject.addProperty("value", jwt.serialize());
		env.putObject("authorization_request_object", requestObject);
	}
}
