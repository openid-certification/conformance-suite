package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CreatePlainCodeChallenge_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreatePlainCodeChallenge cond;

	@Before
	public void setUp(){
		cond = new CreatePlainCodeChallenge();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test(expected = ConditionError.class)
	public void testMissing() {
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate() throws NoSuchAlgorithmException {

		String codeVerifier = "code verifier!";

		env.putString("code_verifier", codeVerifier);

		cond.evaluate(env);

		assertThat(env.getString("code_challenge_method")).isEqualTo("plain");
		assertThat(env.getString("code_challenge")).isNotEmpty();
		assertThat(env.getString("code_challenge")).isEqualTo(codeVerifier);
	}
}
