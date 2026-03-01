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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateRequestObjectClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private ValidateRequestObjectClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateRequestObjectClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		nowSeconds = Instant.now().getEpochSecond();

		JsonObject client = JsonParser.parseString("{ \"client_id\": \"test-client\" }").getAsJsonObject();
		JsonObject server = JsonParser.parseString("{ \"issuer\": \"https://example.com\" }").getAsJsonObject();
		env.putObject("client", client);
		env.putObject("server", server);
	}

	private void addRequestObject(long exp, long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "test-client");
		claims.addProperty("aud", "https://example.com");
		claims.addProperty("exp", exp);
		claims.addProperty("iat", iat);
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void testEvaluate_noError() {
		addRequestObject(nowSeconds + 300, nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(nowSeconds * 1000, nowSeconds - 10);
			cond.execute(env);
		});
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(nowSeconds + 300, nowSeconds - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatRecent() {
		addRequestObject(nowSeconds + 300, nowSeconds - 3600);
		cond.execute(env);
	}
}
