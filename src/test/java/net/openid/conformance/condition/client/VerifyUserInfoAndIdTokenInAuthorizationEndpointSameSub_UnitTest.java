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
public class VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub cond;

	private JsonObject idToken;

	private JsonObject userInfo;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new VerifyUserInfoAndIdTokenInAuthorizationEndpointSameSub();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		idToken = JsonParser.parseString("{"
			+ "\"claims\":{"
				+ "\"sub\":\"1001\","
				+ "\"s_hash\":\"uPd-19KwcgDsqixsclXUjA\","
				+ "\"nonce\":\"R4aIpghKxu\","
				+ "\"aud\":\"899532949009612\""
			+ "}}").getAsJsonObject();

		userInfo = JsonParser.parseString("{"
				+ "\"sub\":\"1001\","
				+ "\"email\":\"john@example.com\","
				+ "\"iss\":\"https://fapidev-as.authlete.net/\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("authorization_endpoint_id_token", idToken);
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_idTokenNull() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("authorization_endpoint_id_token", null);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_subNotFoundInIdToken() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = idToken.getAsJsonObject("claims");
			claims.remove("sub");
			idToken.add("claims", claims);

			env.putObject("authorization_endpoint_id_token", idToken);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_subNotFoundInUserInfo() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("authorization_endpoint_id_token", idToken);

			userInfo.remove("sub");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_userInfoAndIdTokenNotSameSub() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("authorization_endpoint_id_token", idToken);

			userInfo.addProperty("sub", "1002");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

}
