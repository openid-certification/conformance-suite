package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.noContent;

@RunWith(MockitoJUnitRunner.class)
public class UnregisterDynamicallyRegisteredClient_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.delete("/deregister")
			.anyBody()
			.willReturn(noContent()),
		service("bad.example.com")
			.delete("/deregister")
			.anyBody()
			.willReturn(badRequest())));

	private UnregisterDynamicallyRegisteredClient cond;

	@Before
	public void setUp() throws Exception {
		hoverfly.resetJournal();
		cond = new UnregisterDynamicallyRegisteredClient();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test for {@link UnregisterDynamicallyRegisteredClient#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noErrors(){
		env.putString("registration_access_token", "reg.access.token");
		env.putString("registration_client_uri", "https://good.example.com/deregister");
		cond.evaluate(env);
		hoverfly.verify(service("good.example.com")
			.delete("/deregister")
			.header("Authorization", "Bearer reg.access.token"));
	}

	/**
	 * Test for {@link UnregisterDynamicallyRegisteredClient#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badResponse(){
		env.putString("registration_access_token", "reg.access.token");
		env.putString("registration_client_uri", "https://bad.example.com/deregister");
		cond.evaluate(env);
	}


}
