package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RunWith(MockitoJUnitRunner.class)
public class CreateClientAuthenticationAssertionClaimsForRevocationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateClientAuthenticationAssertionClaimsForRevocationEndpoint cond;

	private String clientId = "client";
	private String issuer = "http://example.org/";


	private JsonObject client;

	private JsonObject server;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		client = new JsonObject();
		client.addProperty("client_id", clientId);


		// TODO: Change this to 'revocation_endpoint' when the test changes to use that
		server = new JsonObject();
		server.addProperty("issuer", issuer);

		cond = new CreateClientAuthenticationAssertionClaimsForRevocationEndpoint("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate() {

		env.putObject("client", client);
		env.putObject("server", server);

		cond.evaluate(env);

		assertThat(env.getObject("client_assertion_claims")).isNotNull();

		JsonObject claims = env.getObject("client_assertion_claims");

		assertThat(OIDFJSON.getString(claims.get("iss").getAsJsonPrimitive())).isEqualTo(clientId);
		assertThat(OIDFJSON.getString(claims.get("sub").getAsJsonPrimitive())).isEqualTo(clientId);
		// TODO: Change the aud when we change it in the test.
		assertThat(OIDFJSON.getString(claims.get("aud").getAsJsonPrimitive())).isEqualTo(issuer);

		assertThat(claims.get("jti")).isNotNull();

		Instant now = Instant.now();

		assertThat(OIDFJSON.getLong(claims.get("iat").getAsJsonPrimitive())).isCloseTo(now.getEpochSecond(), within(5L)); // five second leeway
		assertThat(OIDFJSON.getLong(claims.get("exp").getAsJsonPrimitive())).isCloseTo(now.plusSeconds(60).getEpochSecond(), within(5L)); // 60 seconds in the future, 5 second leeway

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClient() {
		env.putObject("server", server);

		cond.evaluate(env);
	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServer() {
		env.putObject("client", client);

		cond.evaluate(env);
	}

}
