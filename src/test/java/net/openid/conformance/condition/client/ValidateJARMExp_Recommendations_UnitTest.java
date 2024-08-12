package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidateJARMExp_Recommendations_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateJARMExpRecommendations cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateJARMExpRecommendations();
		cond.setProperties("UNIT_TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void createResponse(Environment env, int expiresIn) {
		Date now = new Date();
		long nowSeconds = now.getTime() / 1000;

		JsonObject claims = new JsonObject();
		claims.addProperty("exp", nowSeconds + expiresIn);

		JsonObject response = new JsonObject();
		response.add("claims", claims);
		env.putObject("jarm_response",response);
	}

	@Test
	public void testEvaluate_noError() {
		createResponse(env,240); // 4 minute offset
		cond.execute(env);
		verify(env, atLeastOnce()).getLong("jarm_response", "claims.exp");
	}

	@Test
	public void testEvaluate_noErrorMaxLength() {
		createResponse(env,15*60); // 15 minutes (10 minutes allowable in spec + 5 minutes allowed time skew)
		cond.execute(env);
	}

	@Test
	public void testEvaluate_tooLong() {
		assertThrows(ConditionError.class, () -> {
			createResponse(env, 16 * 60); // 16 minutes (10 minutes allowable in spec + 5 minutes allowed time skew)
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_tooShort() {
		assertThrows(ConditionError.class, () -> {
			createResponse(env, 5); // 5 seconds; fairly random
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noExp() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			JsonObject response = new JsonObject();
			response.add("claims", claims);
			env.putObject("jarm_response", response);
			cond.execute(env);
		});
	}
}
