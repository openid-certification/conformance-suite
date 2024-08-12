package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
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

	@Test
	public void testEvaluate_missingClient() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("server", server);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingServer() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("client", client);

			cond.execute(env);
		});
	}

}
