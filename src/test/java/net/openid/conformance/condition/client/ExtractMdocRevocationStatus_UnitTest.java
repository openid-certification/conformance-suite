package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.CborDouble;
import org.multipaz.cbor.DataItemExtensionsKt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ExtractMdocRevocationStatus_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractMdocRevocationStatus cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractMdocRevocationStatus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_readsAValidIndexAsValid() throws Exception {
		useStatusList(StatusListCwtTestFixtures.validStatusListToken());
		// the fixture marks even indices valid
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 40);

		cond.execute(env);

		assertEquals("VALID", env.getString(AbstractRevocationListCwtCondition.ENV_STATUS));
	}

	@Test
	public void testEvaluate_readsARevokedIndexAsInvalidWithoutFailing() throws Exception {
		useStatusList(StatusListCwtTestFixtures.validStatusListToken());
		// the fixture marks odd indices revoked; acting on that is EnsureMdocNotRevoked's job
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 41);

		cond.execute(env);

		assertEquals("INVALID", env.getString(AbstractRevocationListCwtCondition.ENV_STATUS));
	}

	@Test
	public void testEvaluate_failsWithoutAnIndex() throws Exception {
		useStatusList(StatusListCwtTestFixtures.validStatusListToken());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertNull(env.getString(AbstractRevocationListCwtCondition.ENV_STATUS));
	}

	@Test
	public void testEvaluate_failsWhenTheIndexIsOutsideTheList() throws Exception {
		useStatusList(StatusListCwtTestFixtures.validStatusListToken());
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 256);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("Failed to read"), error.getMessage());
		assertNull(env.getString(AbstractRevocationListCwtCondition.ENV_STATUS));
	}

	@Test
	public void testEvaluate_readsTheStatusWhenExpIsAFloatingPointNumber() throws Exception {
		useStatusList(StatusListCwtTestFixtures.statusListTokenWithClaim(StatusListCwt.CLAIM_EXP,
			new CborDouble(Instant.now().getEpochSecond() + 600.5)));
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 40);

		cond.execute(env);

		assertEquals("VALID", env.getString(AbstractRevocationListCwtCondition.ENV_STATUS));
	}

	@Test
	public void testEvaluate_failsWhenTheListHasExpired() throws Exception {
		useStatusList(StatusListCwtTestFixtures.statusListTokenWithClaim(StatusListCwt.CLAIM_EXP,
			DataItemExtensionsKt.toDataItem(Instant.now().getEpochSecond() - 600)));
		env.putInteger(AbstractRevocationListCwtCondition.ENV_STATUS_LIST_IDX, 40);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("expired"), error.getMessage());
	}

	private void useStatusList(byte[] token) {
		env.putString(AbstractRevocationListCwtCondition.ENV_URI, StatusListCwtTestFixtures.DEFAULT_URI);
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, StatusListCwtTestFixtures.encode(token));
	}
}
