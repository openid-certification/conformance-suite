package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyStatusListTokenSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyStatusListTokenSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyStatusListTokenSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token"));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingServerJwks() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject());
		putStatusListToken(createSignedStatusListToken(signingKey));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidSignature() throws Exception {
		RSAKey trustedKey = new RSAKeyGenerator(2048)
			.keyID("trusted-status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
		RSAKey attackerKey = new RSAKeyGenerator(2048)
			.keyID("attacker-status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(trustedKey.toPublicJWK()).toString()).getAsJsonObject());
		putStatusListToken(createSignedStatusListToken(attackerKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureWithTheCredentialsX5cLeafCertificate() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID("credential-signer").generate();
		RSAKey serverKey = new RSAKeyGenerator(2048).keyID("server-key").generate();

		// the issuer's published JWK set does not hold the credential key: the credential's x5c header is what ties it to the issuer
		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(serverKey.toPublicJWK()).toString()).getAsJsonObject());
		putCredentialX5c(signingKey);
		putStatusListToken(createStatusListTokenWithX5c(signingKey, signingKey));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsX5cThatIsNotTheCredentials() throws Exception {
		ECKey credentialKey = new ECKeyGenerator(Curve.P_256).keyID("credential-signer").generate();
		ECKey attackerKey = new ECKeyGenerator(Curve.P_256).keyID("attacker").generate();

		putCredentialX5c(credentialKey);
		putStatusListToken(createStatusListTokenWithX5c(attackerKey, attackerKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsAnotherX5cWhenAStatusListTrustSourceValidatesTheChain() throws Exception {
		ECKey credentialKey = new ECKeyGenerator(Curve.P_256).keyID("credential-signer").generate();
		ECKey statusIssuerKey = new ECKeyGenerator(Curve.P_256).keyID("status-issuer").generate();

		putCredentialX5c(credentialKey);
		env.putString(RegisterStatusListTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED, "true");
		putStatusListToken(createStatusListTokenWithX5c(statusIssuerKey, statusIssuerKey));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsSelfAssertedJwkHeader() throws Exception {
		ECKey credentialKey = new ECKeyGenerator(Curve.P_256).keyID("credential-signer").generate();
		ECKey attackerKey = new ECKeyGenerator(Curve.P_256).keyID("attacker").generate();

		putCredentialX5c(credentialKey);
		env.putString(RegisterStatusListTrustAnchor.ENV_TRUST_SOURCE_CONFIGURED, "true");
		putStatusListToken(createStatusListTokenWithJwk(attackerKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsJwkHeaderInTheIssuersJwks() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID("issuer-key").generate();

		env.putObject("server_jwks",
			JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject());
		putStatusListToken(createStatusListTokenWithJwk(signingKey));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsX5cLeafCertificateThatDidNotSign() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).keyID("credential-signer").generate();
		ECKey otherKey = new ECKeyGenerator(Curve.P_256).keyID("other-key").generate();

		putStatusListToken(createStatusListTokenWithX5c(signingKey, otherKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsWhenNeitherEmbeddedJwkNorServerJwks() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048)
			.keyID("status-list-key")
			.algorithm(JWSAlgorithm.RS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();

		putStatusListToken(createSignedStatusListToken(signingKey));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putCredentialX5c(ECKey credentialKey) throws Exception {
		JsonObject header = new JsonObject();
		JsonArray x5c = new JsonArray();
		x5c.add(Base64.encode(selfSignedCertDer(credentialKey)).toString());
		header.add("x5c", x5c);
		JsonObject credential = new JsonObject();
		credential.add("header", header);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	private String createStatusListTokenWithJwk(ECKey signingKey) throws Exception {
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256).jwk(signingKey.toPublicJWK()).build(),
			statusListClaims());
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}

	private void putStatusListToken(String jwt) throws Exception {
		env.putObject("status_list_token",
			net.openid.conformance.util.JWTUtil.jwtStringToJsonObjectForEnvironment(jwt));
	}

	private String createSignedStatusListToken(RSAKey signingKey) throws Exception {
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
			statusListClaims());
		jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
		return jwt.serialize();
	}

	/** A token signed with {@code signingKey} whose x5c header carries a self-signed certificate of {@code certifiedKey}. */
	private String createStatusListTokenWithX5c(ECKey signingKey, ECKey certifiedKey) throws Exception {
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256)
				.keyID(signingKey.getKeyID())
				.x509CertChain(List.of(Base64.encode(selfSignedCertDer(certifiedKey))))
				.build(),
			statusListClaims());
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}

	private static byte[] selfSignedCertDer(ECKey key) throws Exception {
		X500Name name = new X500Name("CN=Status list signer unit test");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			BigInteger.valueOf(System.nanoTime()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(key.toECPublicKey().getEncoded()));
		return builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey())).getEncoded();
	}

	private static JWTClaimsSet statusListClaims() {
		TokenStatusList tokenStatusList = TokenStatusList.create(
			new byte[] { (byte) TokenStatusList.Status.VALID.getTypeValue() }, 1);
		Instant now = Instant.now();

		return new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(300)))
			.claim("status_list", Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()))
			.build();
	}
}
