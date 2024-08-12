package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateFapiInteractionIdIfNeeded_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateFapiInteractionIdIfNeeded cond;

	private String interactionId = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a"; // example from FAPI spec

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateFapiInteractionIdIfNeeded();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void test_existing() {

		env.putString("fapi_interaction_id", interactionId);
		cond.execute(env);

		verify(env, atLeastOnce()).getString("fapi_interaction_id");
		assertEquals(interactionId, env.getString("fapi_interaction_id"));
	}

	@Test
	public void test_create() {

		cond.execute(env);

		verify(env, atLeastOnce()).getString("fapi_interaction_id");
		verify(env, times(1)).putString(eq("fapi_interaction_id"), anyString());
		assertNotNull(env.getString("fapi_interaction_id"));
		assertNotEquals(interactionId, env.getString("fapi_interaction_id"));

	}

}
