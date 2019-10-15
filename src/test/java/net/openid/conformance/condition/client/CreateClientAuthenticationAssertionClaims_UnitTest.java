package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateClientAuthenticationAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateClientAuthenticationAssertionClaims cond;

	private String clientId = "client";

	private String tokenEndpoint = "https://server.example.com/token";

	private JsonObject client;

	private JsonObject server;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		client = new JsonObject();
		client.addProperty("client_id", clientId);

		server = new JsonObject();
		server.addProperty("token_endpoint", tokenEndpoint);

		cond = new CreateClientAuthenticationAssertionClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate() {

		env.putObject("client", client);
		env.putObject("server", server);

		cond.execute(env);

		assertThat(env.getObject("client_assertion_claims")).isNotNull();

		JsonObject claims = env.getObject("client_assertion_claims");

		assertThat(OIDFJSON.getString(claims.get("iss"))).isEqualTo(clientId);
		assertThat(OIDFJSON.getString(claims.get("sub"))).isEqualTo(clientId);
		assertThat(OIDFJSON.getString(claims.get("aud"))).isEqualTo(tokenEndpoint);

		assertThat(claims.get("jti")).isNotNull();

		Instant now = Instant.now();

		assertThat(OIDFJSON.getLong(claims.get("iat"))).isCloseTo(now.getEpochSecond(), within(5L)); // five second leeway
		assertThat(OIDFJSON.getLong(claims.get("exp"))).isCloseTo(now.plusSeconds(60).getEpochSecond(), within(5L)); // 60 seconds in the future, 5 second leeway

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClient() {
		env.putObject("server", server);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServer() {
		env.putObject("client", client);

		cond.execute(env);
	}

}
