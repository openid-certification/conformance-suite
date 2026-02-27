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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateJARMResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;
	private String clientId;

	private ValidateJARMResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateJARMResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		nowSeconds = Instant.now().getEpochSecond();
		clientId = "test-client";

		env.putObject("client", JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject());
		env.putObject("server", JsonParser.parseString("{ \"issuer\": \"https://example.com\" }").getAsJsonObject());
	}

	private void addJarmResponse(long exp, long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://example.com");
		claims.addProperty("aud", clientId);
		claims.addProperty("exp", exp);
		claims.addProperty("iat", iat);
		JsonObject jarmResponse = new JsonObject();
		jarmResponse.add("claims", claims);
		env.putObject("jarm_response", jarmResponse);
	}

	@Test
	public void testEvaluate_noError() {
		addJarmResponse(nowSeconds + 300, nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addJarmResponse(nowSeconds * 1000, nowSeconds - 10);
			cond.execute(env);
		});
	}

	@Test
	public void testExpReasonable() {
		addJarmResponse(nowSeconds + 3600, nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addJarmResponse(nowSeconds + 300, nowSeconds - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatRecent() {
		addJarmResponse(nowSeconds + 300, nowSeconds - 3600);
		cond.execute(env);
	}
}
