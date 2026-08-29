package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateValidStatusListReference_UnitTest {

	private CreateValidStatusListReference cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateValidStatusListReference();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void testEvaluate_allocatesAnEvenIndexTheListMarksValid() {
		env.putString("base_url", "https://example.com/test/a/alias");

		assertDoesNotThrow(() -> cond.execute(env));

		int idx = OIDFJSON.getInt(
			env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "idx"));
		assertEquals(0, idx % 2, "allocated index should be even");
		assertTrue(idx < EvenOddStatusListContents.STATUS_LIST_ENTRIES);
		assertEquals(TokenStatusList.Status.VALID,
			EvenOddStatusListContents.create().getStatus(idx));
		assertEquals("https://example.com/test/a/alias/" + AbstractCreateStatusListReference.STATUS_LIST_PATH,
			OIDFJSON.getString(
				env.getElementFromObject(AbstractCreateStatusListReference.ENV_KEY, "uri")));
	}

	@Test
	public void testEvaluate_failsWithoutABaseUrl() {
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("base url"), e.getMessage());
	}
}
