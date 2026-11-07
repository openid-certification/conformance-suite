package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void spacesInScopeAreSentAsPlus() {
		Environment env = new Environment();
		JsonObject server = new JsonObject();
		server.addProperty("authorization_endpoint", "https://example.com/oauth/authorize");
		env.putObject("server", server);
		JsonObject request = new JsonObject();
		request.addProperty("scope", "openid email");
		request.addProperty("state", "x+y");
		env.putObject("authorization_endpoint_request", request);
		BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus cond = new BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("redirect_to_authorization_endpoint"))
			.isEqualTo("https://example.com/oauth/authorize?scope=openid+email&state=x%2By");
	}
}
