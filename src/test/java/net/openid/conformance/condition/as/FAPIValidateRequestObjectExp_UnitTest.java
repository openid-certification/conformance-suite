package net.openid.conformance.condition.as;

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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FAPIValidateRequestObjectExp_UnitTest
{

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIValidateRequestObjectExp cond;

	private JsonObject claims;

	private long nowSeconds;


	@BeforeEach
	public void setUp() throws Exception {

		cond = new FAPIValidateRequestObjectExp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server


		claims = JsonParser.parseString("{"
			+ "\"iss\":\"test-client-id-346334adgdsfgdfg3425\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt + 300);

	}

	private void addRequestObjectClaims(Environment env, JsonObject claims) {
		JsonObject requestObjectClaims = new JsonObject();
		requestObjectClaims.getAsJsonObject().add("claims", claims);
		env.putObject("authorization_request_object", requestObjectClaims);
	}


	@Test
	public void testEvaluate_noError() {

		addRequestObjectClaims(env, claims);

		cond.execute(env);

		verify(env, atLeastOnce()).getLong("authorization_request_object", "claims.exp");
	}

	@Test
	public void testEvaluate_missingExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");

			addRequestObjectClaims(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");
			claims.addProperty("exp", nowSeconds - (5 * 60)); // 5 minutes in the past is not ok

			addRequestObjectClaims(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (4 * 60)); // 4 minutes out should be fine still

		addRequestObjectClaims(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_validExpFuture() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds + (60 * 59)); // exp claim time range has not expired (59 minutes)

		addRequestObjectClaims(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_invalidExpFuture() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");
			claims.addProperty("exp", nowSeconds + (60 * 60 * 25)); // exp claim time range has expired (25 hours)

			addRequestObjectClaims(env, claims);

			cond.execute(env);

		});

	}

}
