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
public class ValidateSignedUserInfoResponseStandardJWTClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;
	private String clientId;

	private ValidateSignedUserInfoResponseStandardJWTClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateSignedUserInfoResponseStandardJWTClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		nowSeconds = Instant.now().getEpochSecond();
		clientId = "test-client";

		env.putObject("client", JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject());
		env.putObject("server", JsonParser.parseString("{ \"issuer\": \"https://example.com\" }").getAsJsonObject());
	}

	private void addUserInfoResponse(Long exp, Long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", "https://example.com");
		claims.addProperty("aud", clientId);
		claims.addProperty("sub", "user123");
		if (exp != null) {
			claims.addProperty("exp", exp);
		}
		if (iat != null) {
			claims.addProperty("iat", iat);
		}
		JsonObject userinfoObject = new JsonObject();
		userinfoObject.add("claims", claims);
		env.putObject("userinfo_object", userinfoObject);
	}

	@Test
	public void testEvaluate_noError() {
		addUserInfoResponse(nowSeconds + 300, nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addUserInfoResponse(nowSeconds * 1000, nowSeconds - 10);
			cond.execute(env);
		});
	}

	@Test
	public void testExpReasonable() {
		addUserInfoResponse(nowSeconds + 3600, nowSeconds - 10);
		cond.execute(env);
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addUserInfoResponse(nowSeconds + 300, nowSeconds - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatRecent() {
		addUserInfoResponse(nowSeconds + 300, nowSeconds - 3600);
		cond.execute(env);
	}

	@Test
	public void testNoExpOrIat() {
		addUserInfoResponse(null, null);
		cond.execute(env);
	}
}
