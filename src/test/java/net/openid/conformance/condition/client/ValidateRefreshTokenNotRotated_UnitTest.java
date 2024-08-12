package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateRefreshTokenNotRotated_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenResponse;

	private ValidateRefreshTokenNotRotated cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateRefreshTokenNotRotated();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Example from RFC6750
		tokenResponse = JsonParser.parseString("{" +
			"\"access_token\":\"mF_9.B5f-4.1JqM\"," +
			"\"token_type\":\"Bearer\"," +
			"\"expires_in\":3600," +
			"\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\"" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate_sameRefreshToken() {
		env.putObject("token_endpoint_response", tokenResponse);
		env.putString("refresh_token", "tGzv3JOkF0XG5Qx2TlKWIA");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noNewRefreshToken() {
		env.putObject("token_endpoint_response", new JsonObject());
		env.putString("refresh_token", "tGzv3JOkF0XG5Qx2TlKWIA");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_differentRefreshToken() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("token_endpoint_response", tokenResponse);
			env.putString("refresh_token", "foobar");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noExistingRefreshToken() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("token_endpoint_response", tokenResponse);
			cond.execute(env);
		});
	}

}
