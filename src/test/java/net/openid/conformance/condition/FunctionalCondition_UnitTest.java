package net.openid.conformance.condition;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FunctionalCondition_UnitTest {

	private Environment env;
	private TestInstanceEventLog eventLog;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		eventLog = mock(TestInstanceEventLog.class);
	}

	private FunctionalCondition create(String name, FunctionalCondition.Body body) {
		FunctionalCondition condition = new FunctionalCondition(name, body);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE, "REQ-1");
		return condition;
	}

	@Test
	public void success_isLoggedUnderTheConditionName() {
		env.putString("nonce", "abc");

		create("EnsureNonceIsPresent", (e, c) -> c.logSuccess("nonce present", Map.of("nonce", e.getString("nonce"))))
			.execute(env);

		verify(eventLog).log(eq("EnsureNonceIsPresent"), anyMap());
	}

	@Test
	public void error_isLoggedAndThrownUnderTheConditionName() {
		FunctionalCondition condition = create("EnsureNonceIsPresent", (e, c) -> {
			throw c.error("nonce missing");
		});

		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));

		assertTrue(error.getMessage().startsWith("EnsureNonceIsPresent: nonce missing"), error.getMessage());
		verify(eventLog).log(eq("EnsureNonceIsPresent"), anyMap());
	}

	@Test
	public void environmentChangesAreVisibleAfterExecution() {
		create("StoreValue", (e, c) -> {
			e.putString("stored", "value");
			c.log("stored value");
		}).execute(env);

		assertEquals("value", env.getString("stored"));
	}

	@Test
	public void nameIsRequired() {
		assertThrows(NullPointerException.class, () -> new FunctionalCondition(null, (e, c) -> c.log("x")));
	}
}
