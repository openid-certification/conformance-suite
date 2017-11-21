package io.fintechlabs.testframework.condition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateRandomNonceValue_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private CreateRandomNonceValue cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new CreateRandomNonceValue("UNIT-TEST", eventLog, false);
	}

	/**
	 */
	@Test
	public void testEvaluate() {
		
		cond.evaluate(env);
		
		assertThat(env.getString("nonce")).isNotEmpty();
	}
}
