package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateDynamicRegistrationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;


	private CreateDynamicRegistrationRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new CreateDynamicRegistrationRequest("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noClientName(){
		System.out.println("noName: " + env);
		cond.evaluate(env);
		assertThat(env.get("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("UNIT-TEST");
	}

	@Test
	public void testEvaluate_withClientName(){
		env.putString("client_name","my-client-name");
		System.out.println("withName: " + env);
		cond.evaluate(env);
		assertThat(env.get("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("my-client-name UNIT-TEST");
	}

}
