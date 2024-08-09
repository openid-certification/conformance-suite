package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
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

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class FetchServerKeys_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static String jwksStr = "{"
		+ "\"keys\":["
		+ "{"
		+ "\"kty\":\"oct\","
		+ "\"alg\":\"HS256\","
		+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
		+ "},"
		+ "{"
		+ "\"kty\":\"oct\","
		+ "\"alg\":\"HS256\","
		+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
		+ "}"
		+ "]}";

	private static String unsupportedJwksStr = "{"
		+ "\"keys\":["
		+ "{"
		+ "\"crv\":\"secp256k1\","
		+ "\"x\":\"lp8T17Y1LosMIOQmxWb7N62szWQeG-_bzb7R8e9clLI\","
		+ "\"y\":\"mXYsyG_rC8w41f9oC9XPWknFtCCpRM9iHQP7GY24MD8\","
		+ "\"kty\":\"EC\","
		+ "\"use\":\"sig\","
		+ "\"kid\":\"Rqu-16ARNH_Lgt4AtqFJDgsFlQLVOtUavMrg8Plj5U0\""
		+ "}"
		+ "]}";

	private FetchServerKeys cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {

		hoverfly.simulate(dsl(
			service("good.example.com")
				.get("/jwks.json")
				.willReturn(success(jwksStr, "application/json")),
			service("good.example1.com")
				.get("/jwks.json")
				.willReturn(success(unsupportedJwksStr, "application/json")),
			service("bad.example.com")
				.get("/jwks.json")
				.willReturn(success("This is not JSON!", "text/plain"))));
		hoverfly.resetJournal();

		cond = new FetchServerKeys();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link FetchServerKeys#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_dynamic(Hoverfly hoverfly) {

		JsonObject jwks = JsonParser.parseString(jwksStr).getAsJsonObject();

		JsonObject server = new JsonObject();
		server.addProperty("jwks_uri", "https://good.example.com/jwks.json");
		env.putObject("server", server);

		cond.execute(env);

		hoverfly.verify(service("good.example.com").get("/jwks.json"));

		assertThat(env.getObject("server_jwks")).isEqualTo(jwks);
	}

	@Test
	public void testEvaluate_dynamicWithUnsupportedServerJWKs(Hoverfly hoverfly) {

		JsonObject jwks = JsonParser.parseString(unsupportedJwksStr).getAsJsonObject();

		JsonObject server = new JsonObject();
		server.addProperty("jwks_uri", "https://good.example1.com/jwks.json");
		env.putObject("server", server);

		cond.execute(env);

		hoverfly.verify(service("good.example1.com").get("/jwks.json"));

		assertThat(env.getObject("server_jwks")).isEqualTo(jwks);
	}

	/**
	 * Test method for {@link FetchServerKeys#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingUri() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("server", new JsonObject());

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link FetchServerKeys#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingConfig() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link FetchServerKeys#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noServer() {
		assertThrows(ConditionError.class, () -> {

			JsonObject server = new JsonObject();
			server.addProperty("jwks_uri", "https://nonexisting.example.com/jwks.json");
			env.putObject("server", server);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link FetchServerKeys#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badResponse() {
		assertThrows(ConditionError.class, () -> {

			JsonObject server = new JsonObject();
			server.addProperty("jwks_uri", "https://bad.example.com/jwks.json");
			env.putObject("server", server);

			cond.execute(env);
		});
	}
}
