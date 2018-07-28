package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateS256CodeChallenge_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateS256CodeChallenge cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateS256CodeChallenge("UNIT-TEST", eventLog, ConditionResult.INFO);
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

		assertThat(env.getString("code_challenge_method")).isEqualTo("S256");

		String res = env.getString("code_challenge");

		assertThat(res).isNotEmpty();

		byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		String challenge = Base64.encodeBase64URLSafeString(digest);

		assertThat(res).isEqualTo(challenge);

	}
}
