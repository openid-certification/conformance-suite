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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CheckMdocCredentialStatus_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckMdocCredentialStatus cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckMdocCredentialStatus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(StatusListCwtTestFixtures.validStatusListToken()));
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_URI,
			StatusListCwtTestFixtures.DEFAULT_URI);
	}

	@Test
	public void testEvaluate_acceptsValidIndex() {
		// the fixture marks even indices valid
		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, 40);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsRevokedIndex() {
		// the fixture marks odd indices revoked
		env.putInteger(AbstractStatusListCwtCondition.ENV_STATUS_LIST_IDX, 41);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("revoked"), error.getMessage());
	}

	@Test
	public void testEvaluate_failsWithoutAnIndex() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
