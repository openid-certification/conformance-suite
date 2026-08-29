package net.openid.conformance.vci10wallet.condition.statuslist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractStatusListCwtCondition;
import net.openid.conformance.condition.client.CheckMdocCredentialStatus;
import net.openid.conformance.condition.client.ValidateStatusListTokenCwtFormat;
import net.openid.conformance.condition.client.VerifyStatusListTokenCwtSignature;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIGenerateCwtStatusListToken_UnitTest {

	private static final String ISSUER = "https://issuer.example.com/";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIGenerateCwtStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIGenerateCwtStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("server", "issuer", ISSUER);
		env.putString("current_status_list_id", "1");
	}

	@Test
	public void testEvaluate_generatesTokenTheConsumptionConditionsAccept() throws Exception {
		env.putObject("server_jwks", jwksWithCertifiedEcKey());

		cond.execute(env);

		String token = env.getString("current_status_list_cwt");
		assertNotNull(token);

		// hand the generated token to the conditions that consume an MSO revocation list
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN, token);
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI, ISSUER + "statuslists/1");

		assertDoesNotThrow(() -> run(new ValidateStatusListTokenCwtFormat()));
		assertDoesNotThrow(() -> run(new VerifyStatusListTokenCwtSignature()));

		// the generated list marks even indices valid and odd indices revoked
		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, 12);
		assertDoesNotThrow(() -> run(new CheckMdocCredentialStatus()));

		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, 13);
		assertThrows(ConditionError.class, () -> run(new CheckMdocCredentialStatus()));
	}

	@Test
	public void testEvaluate_rejectsJwksWithoutAnEcPrivateKey() {
		env.putObject("server_jwks", JsonParser.parseString("{\"keys\":[]}").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void run(net.openid.conformance.condition.Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		condition.execute(env);
	}

	private JsonObject jwksWithCertifiedEcKey() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();

		X500Name name = new X500Name("C=UT,CN=OIDF Status List Signer");
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

		ECKey withChain = new ECKey.Builder(key)
			.x509CertChain(List.of(com.nimbusds.jose.util.Base64.encode(der)))
			.build();

		JsonObject jwks = new JsonObject();
		jwks.add("keys", JsonParser.parseString("[" + withChain.toJSONString() + "]").getAsJsonArray());
		return jwks;
	}
}
