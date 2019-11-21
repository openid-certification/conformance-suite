package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddMTLSEndpointAliasesToEnvironment_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddMTLSEndpointAliasesToEnvironment cond;

	@Before
	public void setUp() throws Exception {

		cond = new AddMTLSEndpointAliasesToEnvironment();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
			+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
			+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
			+ "\"mtls_endpoint_aliases\": {"
			+ 		"\"token_endpoint\": \"https://mtls.example.com/token\","
			+ 		"\"revocation_endpoint\": \"https://mtls.example.com/revo\","
			+ 		"\"introspection_endpoint\": \"https://mtls.example.com/introspect\""
			+ "}}").getAsJsonObject();

		env.putObject("server", server);

		cond.execute(env);

		assertThat(env.getString("server", "mtls_endpoint_aliases.token_endpoint")).isEqualTo(env.getString("token_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.revocation_endpoint")).isEqualTo(env.getString("revocation_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.introspection_endpoint")).isEqualTo(env.getString("introspection_endpoint"));
		assertThat(env.getString("server", "userinfo_endpoint")).isEqualTo(env.getString("userinfo_endpoint"));

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notPresentMtlsEndpointAliases() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\""
			+ "}").getAsJsonObject();

		env.putObject("server", server);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingConfig() {
		cond.execute(env);
	}
}
