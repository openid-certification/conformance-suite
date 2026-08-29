package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractStatusListCwtCondition;
import net.openid.conformance.condition.client.CheckMdocCredentialStatus;
import net.openid.conformance.condition.client.ValidateStatusListSignerCertificateProfile;
import net.openid.conformance.condition.client.ValidateStatusListTokenCwtFormat;
import net.openid.conformance.condition.client.VerifyStatusListTokenCwtSignature;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VP1FinalGenerateCwtStatusListToken_UnitTest {

	private static final String STATUS_LIST_URI =
		"https://localhost.emobix.co.uk:8443/test/a/alias/statuslists/1";
	private static final int REVOKED_IDX = 41;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalGenerateCwtStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new VP1FinalGenerateCwtStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject reference = new JsonObject();
		reference.addProperty("uri", STATUS_LIST_URI);
		reference.addProperty("idx", REVOKED_IDX);
		env.putObject(AbstractCreateStatusListReference.ENV_KEY, reference);
	}

	@Test
	public void testEvaluate_generatesAnMsoRevocationListTheConsumptionConditionsAccept() {
		cond.execute(env);

		String token = env.getString(VP1FinalGenerateCwtStatusListToken.ENV_KEY);
		assertThat(token).isNotNull();

		// hand the generated token to the conditions that consume an MSO revocation list
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN, token);
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI, STATUS_LIST_URI);

		assertDoesNotThrow(() -> run(new ValidateStatusListTokenCwtFormat()));
		assertDoesNotThrow(() -> run(new VerifyStatusListTokenCwtSignature()));
		// ISO/IEC 18013-5 Table B.9
		assertDoesNotThrow(() -> run(new ValidateStatusListSignerCertificateProfile()));
	}

	@Test
	public void testEvaluate_marksTheReferencedIndexRevokedAndEvenIndicesValid() {
		cond.execute(env);

		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			env.getString(VP1FinalGenerateCwtStatusListToken.ENV_KEY));
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI, STATUS_LIST_URI);

		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, REVOKED_IDX);
		assertThrows(ConditionError.class, () -> run(new CheckMdocCredentialStatus()));

		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, REVOKED_IDX - 1);
		assertDoesNotThrow(() -> run(new CheckMdocCredentialStatus()));
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
