package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class FAPICIBAValidateRtHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPICIBAValidateRtHash cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPICIBAValidateRtHash();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject token = JsonParser.parseString("{\"refresh_token\": \"4bwc0ESC_IAhflf-ACC_vjD_ltc11ne-8gFPfA2Kx16\"}").getAsJsonObject();
		env.putObject("token_endpoint_response", token);

		JsonObject rtHash = JsonParser.parseString("{\"alg\":\"RS256\", \"rt_hash\":\"sHahCuSpXCRg5mkDDvvr4w\"}").getAsJsonObject();
		env.putObject("rt_hash", rtHash);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseRefreshTokenEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject token = JsonParser.parseString("{}").getAsJsonObject();
			env.putObject("token_endpoint_response", token);

			JsonObject rtHash = JsonParser.parseString("{\"alg\":\"RS256\", \"rt_hash\":\"sHahCuSpXCRg5mkDDvvr4w\"}").getAsJsonObject();
			env.putObject("rt_hash", rtHash);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_caseBad() {
		assertThrows(ConditionError.class, () -> {
			JsonObject token = JsonParser.parseString("{\"refresh_token\": \"G5kXH2wHvUra0sHlDy1iTkDJgsgUO1bN\"}").getAsJsonObject();
			env.putObject("token_endpoint_response", token);

			JsonObject rtHash = JsonParser.parseString("{\"alg\":\"RS256\", \"rt_hash\":\"sHahCuSpXCRg5mkDDvvr4w\"}").getAsJsonObject();
			env.putObject("rt_hash", rtHash);

			cond.execute(env);

		});

	}
}
