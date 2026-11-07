package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SendAuthorizationResponseWithResponseModeQuery_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private Environment env;
	private SendAuthorizationResponseWithResponseModeQuery cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		cond = new SendAuthorizationResponseWithResponseModeQuery();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void stateWithPlusIsSentAsPercent2B() {
		JsonObject params = new JsonObject();
		params.addProperty("redirect_uri", "https://client.example.com/cb?app=1");
		params.addProperty("code", "SplxlOBeZQQYbYS6WxSbIA");
		params.addProperty("state", "a+b/c=");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, params);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_response_redirect"))
			.isEqualTo("https://client.example.com/cb?app=1&code=SplxlOBeZQQYbYS6WxSbIA&state=a%2Bb%2Fc%3D");
	}
}
