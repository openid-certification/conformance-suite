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
public class ValidateLogoutTokenClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;
	private String clientId;

	private ValidateLogoutTokenClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateLogoutTokenClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		nowSeconds = Instant.now().getEpochSecond();
		clientId = "test-client";

		env.putObject("client", JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject());
		env.putObject("server", JsonParser.parseString("{ \"issuer\": \"https://example.com\" }").getAsJsonObject());
	}

	private void addLogoutToken(long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://example.com");
		claims.addProperty("aud", clientId);
		claims.addProperty("iat", iat);
		claims.addProperty("jti", "unique-jti-value");
		claims.addProperty("sub", "user123");

		JsonObject events = new JsonObject();
		events.add("http://schemas.openid.net/event/backchannel-logout", new JsonObject());
		claims.add("events", events);

		JsonObject logoutToken = new JsonObject();
		logoutToken.add("claims", claims);
		env.putObject("logout_token", logoutToken);
	}

	@Test
	public void testEvaluate_noError() {
		addLogoutToken(nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addLogoutToken(nowSeconds - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatRecent() {
		addLogoutToken(nowSeconds - 3600);
		cond.execute(env);
	}

	@Test
	public void testIatInFuture() {
		assertThrows(ConditionError.class, () -> {
			addLogoutToken(nowSeconds + 3600);
			cond.execute(env);
		});
	}
}
