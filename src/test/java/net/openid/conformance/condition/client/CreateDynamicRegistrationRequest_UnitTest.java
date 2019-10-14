package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CreateDynamicRegistrationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;


	private CreateDynamicRegistrationRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new CreateDynamicRegistrationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test for {@link CreateDynamicRegistrationRequest#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noClientName(){
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("UNIT-TEST");
	}

	/**
	 * Test for {@link CreateDynamicRegistrationRequest#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_withClientName(){
		env.putString("client_name","my-client-name");
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("my-client-name UNIT-TEST");
	}

}
