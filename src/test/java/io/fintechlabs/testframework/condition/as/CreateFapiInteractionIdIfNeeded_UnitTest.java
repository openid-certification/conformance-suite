package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class CreateFapiInteractionIdIfNeeded_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateFapiInteractionIdIfNeeded cond;

	private String interactionId = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a"; // example from FAPI spec

	@Before
	public void setUp() throws Exception {
		cond = new CreateFapiInteractionIdIfNeeded();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void test_existing() {

		env.putString("fapi_interaction_id", interactionId);
		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("fapi_interaction_id");
		assertEquals(interactionId, env.getString("fapi_interaction_id"));
	}

	@Test
	public void test_create() {

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("fapi_interaction_id");
		verify(env, times(1)).putString(eq("fapi_interaction_id"), anyString());
		assertNotNull(env.getString("fapi_interaction_id"));
		assertNotEquals(interactionId, env.getString("fapi_interaction_id"));

	}

}
