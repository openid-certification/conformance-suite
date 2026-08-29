package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VP1FinalGenerateJwtStatusListToken_UnitTest {

	private static final String STATUS_LIST_URI =
		"https://localhost.emobix.co.uk:8443/test/a/alias/statuslists/1";
	private static final int REVOKED_IDX = 41;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalGenerateJwtStatusListToken cond;

	private ECKey credentialSigningKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VP1FinalGenerateJwtStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject reference = new JsonObject();
		reference.addProperty("uri", STATUS_LIST_URI);
		reference.addProperty("idx", REVOKED_IDX);
		env.putObject(CreateRevokedStatusListReference.ENV_KEY, reference);

		credentialSigningKey = certifiedEcKey();
		env.putObject("config", config(credentialSigningKey));
	}

	@Test
	public void testEvaluate_signsTheStatusListWithTheCredentialSigningKeyAndItsCertificateChain()
			throws Exception {
		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString(VP1FinalGenerateJwtStatusListToken.ENV_KEY));

		assertThat(jwt.getHeader().getType().toString()).isEqualTo("statuslist+jwt");
		assertThat(jwt.getHeader().getX509CertChain())
			.isEqualTo(credentialSigningKey.getX509CertChain());
		assertThat(jwt.verify(new ECDSAVerifier(credentialSigningKey.toPublicJWK()))).isTrue();

		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(STATUS_LIST_URI);
		assertThat(jwt.getJWTClaimsSet().getExpirationTime()).isNotNull();
	}

	@Test
	public void testEvaluate_marksTheReferencedIndexRevokedAndEvenIndicesValid() throws Exception {
		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString(VP1FinalGenerateJwtStatusListToken.ENV_KEY));
		JsonObject statusListClaim = JsonParser
			.parseString(jwt.getJWTClaimsSet().getJSONObjectClaim("status_list").toString())
			.getAsJsonObject();

		TokenStatusList statusList = TokenStatusList.decode(
			OIDFJSON.getString(statusListClaim.get("lst")),
			OIDFJSON.getInt(statusListClaim.get("bits")));

		assertThat(statusList.getStatus(REVOKED_IDX)).isEqualTo(TokenStatusList.Status.INVALID);
		assertThat(statusList.getStatus(REVOKED_IDX - 1)).isEqualTo(TokenStatusList.Status.VALID);
	}

	@Test
	public void testEvaluate_failsWithoutACredentialSigningJwk() {
		env.putObject("config", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private JsonObject config(ECKey signingKey) {
		JsonObject credential = new JsonObject();
		credential.add("signing_jwk", JsonParser.parseString(signingKey.toJSONString()));
		JsonObject config = new JsonObject();
		config.add("credential", credential);
		return config;
	}

	private ECKey certifiedEcKey() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();

		X500Name name = new X500Name("C=UT,CN=OIDF SD-JWT VC issuer");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			new BigInteger(80, new SecureRandom()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(key.toECPublicKey().getEncoded()));
		JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
		builder.addExtension(Extension.subjectKeyIdentifier, false,
			extensionUtils.createSubjectKeyIdentifier(key.toECPublicKey()));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
		byte[] der = builder
			.build(new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey()))
			.getEncoded();

		return new ECKey.Builder(key)
			.x509CertChain(List.of(com.nimbusds.jose.util.Base64.encode(der)))
			.build();
	}
}
