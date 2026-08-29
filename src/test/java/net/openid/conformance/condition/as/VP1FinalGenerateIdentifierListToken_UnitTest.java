package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractRevocationListCwtCondition;
import net.openid.conformance.condition.client.EnsureMdocNotRevoked;
import net.openid.conformance.condition.client.ExtractMdocRevocationStatus;
import net.openid.conformance.condition.client.ValidateMdocRevocationListCwtFormat;
import net.openid.conformance.condition.client.ValidateMdocRevocationListSignerCertificateProfile;
import net.openid.conformance.condition.client.VerifyMdocRevocationListCwtSignature;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
		env.putObject(RevocationListReference.ENV_KEY, reference);
	}

	@Test
	public void testEvaluate_generatesAnIdentifierListTheConsumptionConditionsAccept() {
		cond.execute(env);

		String token = env.getString(ServedRevocationList.ENV_KEY, "token");
		assertThat(token).isNotNull();

		// hand the generated token to the conditions that consume an identifier list
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, token);
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, IDENTIFIER_LIST_URI);
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "identifier_list");

		assertDoesNotThrow(() -> run(new ValidateMdocRevocationListCwtFormat()));
		assertDoesNotThrow(() -> run(new VerifyMdocRevocationListCwtSignature()));
		// ISO/IEC 18013-5 Table B.9
		assertDoesNotThrow(() -> run(new ValidateMdocRevocationListSignerCertificateProfile()));
	}

	@Test
	public void testEvaluate_listsTheAllocatedIdentifierAndNotOthers() {
		cond.execute(env);

		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN,
			env.getString(ServedRevocationList.ENV_KEY, "token"));
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, IDENTIFIER_LIST_URI);
		env.putString(AbstractRevocationListCwtCondition.ENV_MECHANISM, "identifier_list");

		env.putString(AbstractRevocationListCwtCondition.ENV_IDENTIFIER_LIST_ID,
			Base64.getEncoder().encodeToString(REVOKED_IDENTIFIER));
		assertDoesNotThrow(() -> run(new ExtractMdocRevocationStatus()));
		assertThrows(ConditionError.class, () -> run(new EnsureMdocNotRevoked()));

		env.putString(AbstractRevocationListCwtCondition.ENV_IDENTIFIER_LIST_ID,
			Base64.getEncoder().encodeToString(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 }));
		assertDoesNotThrow(() -> run(new ExtractMdocRevocationStatus()));
		assertDoesNotThrow(() -> run(new EnsureMdocNotRevoked()));
	}

	private void run(Condition condition) {
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		condition.execute(env);
	}
}
