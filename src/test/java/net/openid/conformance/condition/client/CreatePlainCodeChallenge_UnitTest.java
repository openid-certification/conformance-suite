package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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
		cond.execute(env);
	}

	@Test
	public void testEvaluate() throws NoSuchAlgorithmException {

		String codeVerifier = "code verifier!";

		env.putString("code_verifier", codeVerifier);

		cond.execute(env);

		assertThat(env.getString("code_challenge_method")).isEqualTo("plain");
		assertThat(env.getString("code_challenge")).isNotEmpty();
		assertThat(env.getString("code_challenge")).isEqualTo(codeVerifier);
	}
}
