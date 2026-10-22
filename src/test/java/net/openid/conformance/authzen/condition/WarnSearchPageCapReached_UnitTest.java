package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnSearchPageCapReached_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnSearchPageCapReached cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnSearchPageCapReached(100);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void tokenOutstanding_isFlagged() {
		env.putString("authzen_search_endpoint_request_page_token", "page-101");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("after 100 pages"), e.getMessage());
	}
}
