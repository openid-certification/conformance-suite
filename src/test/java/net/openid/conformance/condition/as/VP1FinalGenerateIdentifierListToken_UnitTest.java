package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractIdentifierListCwtCondition;
import net.openid.conformance.condition.client.CheckMdocCredentialIdentifierListStatus;
import net.openid.conformance.condition.client.ValidateIdentifierListSignerCertificateProfile;
import net.openid.conformance.condition.client.ValidateIdentifierListTokenCwtFormat;
import net.openid.conformance.condition.client.VerifyIdentifierListTokenCwtSignature;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VP1FinalGenerateIdentifierListToken_UnitTest {

	private static final String IDENTIFIER_LIST_URI =
		"https://localhost.emobix.co.uk:8443/test/a/alias/identifierlists/1";
	private static final byte[] REVOKED_IDENTIFIER = { 10, 20, 30, 40, 50, 60, 70, 80 };

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalGenerateIdentifierListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new VP1FinalGenerateIdentifierListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject reference = new JsonObject();
		reference.addProperty("uri", IDENTIFIER_LIST_URI);
		reference.addProperty("id", Base64.getEncoder().encodeToString(REVOKED_IDENTIFIER));
		env.putObject(CreateRevokedIdentifierListReference.ENV_KEY, reference);
	}

	@Test
	public void testEvaluate_generatesAnIdentifierListTheConsumptionConditionsAccept() {
		cond.execute(env);

		String token = env.getString(VP1FinalGenerateIdentifierListToken.ENV_KEY);
		assertThat(token).isNotNull();

		// hand the generated token to the conditions that consume an identifier list
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN, token);
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI, IDENTIFIER_LIST_URI);

		assertDoesNotThrow(() -> run(new ValidateIdentifierListTokenCwtFormat()));
		assertDoesNotThrow(() -> run(new VerifyIdentifierListTokenCwtSignature()));
		// ISO/IEC 18013-5 Table B.9
		assertDoesNotThrow(() -> run(new ValidateIdentifierListSignerCertificateProfile()));
	}

	@Test
	public void testEvaluate_listsTheAllocatedIdentifierAndNotOthers() {
		cond.execute(env);

		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			env.getString(VP1FinalGenerateIdentifierListToken.ENV_KEY));
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI, IDENTIFIER_LIST_URI);

		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID,
			Base64.getEncoder().encodeToString(REVOKED_IDENTIFIER));
		assertThrows(ConditionError.class, () -> run(new CheckMdocCredentialIdentifierListStatus()));

		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID,
			Base64.getEncoder().encodeToString(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 }));
		assertDoesNotThrow(() -> run(new CheckMdocCredentialIdentifierListStatus()));
	}

	@Test
	public void testEvaluate_signerCertificateChainsToTheIacaThatIssuedTheDocumentSigner()
			throws Exception {
		cond.execute(env);

		// ISO/IEC 18013-5 12.3.6.2: with no Certificate element in the MSO's status element, the
		// revocation list's signer must be certified by the CA that certified the document signer
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate signerCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getStatusListSignerCert().getEncoded())));
		X509Certificate iacaCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getIacaRootCert().getEncoded())));
		X509Certificate dsCert = (X509Certificate) cf.generateCertificate(
			new ByteArrayInputStream(bytes(TestKeysAndCerts.getDocumentSignerCert().getEncoded())));

		signerCert.verify(iacaCert.getPublicKey());
		assertThat(signerCert.getIssuerX500Principal()).isEqualTo(dsCert.getIssuerX500Principal());
	}

	private void run(Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		condition.execute(env);
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
