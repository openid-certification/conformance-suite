package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatePostLogoutRedirectUriRedirect_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private Environment env;
	private CreatePostLogoutRedirectUriRedirect cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		cond = new CreatePostLogoutRedirectUriRedirect();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void stateWithPlusIsSentAsPercent2B() {
		JsonObject params = new JsonObject();
		params.addProperty("state", "a+b c");
		env.putObject("post_logout_redirect_uri_params", params);
		JsonObject request = new JsonObject();
		request.addProperty("post_logout_redirect_uri", "https://rp.example.com/logged-out");
		env.putObject("end_session_endpoint_http_request_params", request);

		cond.execute(env);

		assertThat(env.getString("post_logout_redirect_uri_redirect"))
			.isEqualTo("https://rp.example.com/logged-out?state=a%2Bb%20c");
	}
}
