package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
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
public class CheckMdocCredentialIdentifierListStatus_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckMdocCredentialIdentifierListStatus cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckMdocCredentialIdentifierListStatus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI,
			IdentifierListCwtTestFixtures.DEFAULT_URI);
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(
				IdentifierListCwtTestFixtures.validIdentifierListToken()));
	}

	@Test
	public void testEvaluate_failsWhenTheMsoIdentifierIsOnTheList() {
		putIdentifier(IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("has been revoked");
	}

	@Test
	public void testEvaluate_passesWhenTheMsoIdentifierIsNotOnTheList() {
		putIdentifier(IdentifierListCwtTestFixtures.UNLISTED_IDENTIFIER);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesAgainstAnEmptyIdentifierList() throws Exception {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(
				IdentifierListCwtTestFixtures.emptyIdentifierListToken()));
		putIdentifier(IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenTheTokenCarriesNoIdentifierListClaim() throws Exception {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(
				IdentifierListCwtTestFixtures.identifierListTokenWithoutIdentifierListClaim()));
		putIdentifier(IdentifierListCwtTestFixtures.LISTED_IDENTIFIER);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("key 65530");
	}

	private void putIdentifier(byte[] identifier) {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_ID,
			Base64.getEncoder().encodeToString(identifier));
	}
}
