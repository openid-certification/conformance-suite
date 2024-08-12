package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddBasicAuthClientSecretAuthenticationParameters_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private String expectedAuth;

	private AddBasicAuthClientSecretAuthenticationParameters cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddBasicAuthClientSecretAuthenticationParameters();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Example values from RFC 6749

		client = JsonParser.parseString("{"
			+ "\"client_id\":\"s6BhdRkqt3\","
			+ "\"client_secret\":\"7Fjfp0ZBr1KtDRbnfVdmIw\""
			+ "}").getAsJsonObject();

		expectedAuth = "Basic czZCaGRSa3F0Mzo3RmpmcDBaQnIxS3REUmJuZlZkbUl3";

		env.putObject("client", client);
	}

	/**
	 * Test method for {@link AddBasicAuthClientSecretAuthenticationParameters#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("client", "client_secret");

		assertThat(env.getString("token_endpoint_request_headers", "Authorization")).isEqualTo(expectedAuth);
	}

	@Test
	public void testEvaluate_noErrorUrlEncoding() {
		JsonObject clientWithSpecialChars = JsonParser.parseString("{"
			+ "\"client_id\":\"s6BhdRkqt3%\","
			+ "\"client_secret\":\"7Fjfp0Z%Br1KtDRbnfVdmIw%\""
			+ "}").getAsJsonObject();

		String expectedAuthHeader = "Basic czZCaGRSa3F0MyUyNTo3RmpmcDBaJTI1QnIxS3REUmJuZlZkbUl3JTI1";
		env.putObject("client", clientWithSpecialChars);
		cond.execute(env);
		String actualAuthHeader = env.getString("token_endpoint_request_headers", "Authorization");
		assertThat(actualAuthHeader).isEqualTo(expectedAuthHeader);
	}
}
