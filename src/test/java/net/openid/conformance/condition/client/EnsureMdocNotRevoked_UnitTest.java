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
public class EnsureMdocNotRevoked_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMdocNotRevoked cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocNotRevoked();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, StatusListCwtTestFixtures.DEFAULT_URI);
	}

	@Test
	public void testEvaluate_passesWhenTheStatusIsValid() {
		env.putString(AbstractRevocationListCwtCondition.ENV_STATUS, "VALID");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenTheStatusIsInvalid() {
		env.putString(AbstractRevocationListCwtCondition.ENV_STATUS, "INVALID");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("revoked"), error.getMessage());
	}

	@Test
	public void testEvaluate_failsOnAnyOtherStatus() {
		// ISO/IEC 18013-5 12.3.6.1: an mdoc uses no status besides revoked
		env.putString(AbstractRevocationListCwtCondition.ENV_STATUS, "SUSPENDED");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenNoStatusWasRead() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
