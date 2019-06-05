package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckHeartServerJwksFields_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject keyA;
	private JsonObject keyB;

	private CheckHeartServerJwksFields cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckHeartServerJwksFields();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		keyA = new JsonParser().parse("{"
			+ "\"alg\": \"RS256\","
			+ "\"e\": \"AQAB\","
			+ "\"n\": \"o80vbR0ZfMhjZWfqwPUGNkcIeUcweFyzB2S2T-hje83IOVct8gVg9FxvHPK1R"
			+ "eEW3-p7-A8GNcLAuFP_8jPhiL6LyJC3F10aV9KPQFF-w6Eq6VtpEgYSfzvFegNiPtpMWd7C43"
			+ "EDwjQ-GrXMVCLrBYxZC-P1ShyxVBOzeR_5MTC0JGiDTecr_2YT6o_3aE2SIJu4iNPgGh9Mnyx"
			+ "dBo0Uf0TmrqEIabquXA1-V8iUihwfI8qjf3EujkYi7gXXelIo4_gipQYNjr4DBNlE0__RI0kD"
			+ "U-27mb6esswnP2WgHZQPsk779fTcNDBIcYgyLujlcUATEqfCaPDNp00J6AbY6w\","
			+ "\"kty\": \"RSA\","
			+ "\"kid\": \"rsa1\""
			+ "}").getAsJsonObject();

		keyB = new JsonParser().parse("{"
			+ "\"kty\":\"EC\","
			+ "\"crv\":\"P-256\","
			+ "\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\","
			+ "\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\","
			+ "\"use\":\"enc\","
			+ "\"alg\":\"ES256\","
			+ "\"kid\":\"ec1\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noError_singleKey(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keys.add(keyA);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}

	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noError_multipleKey(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keys.add(keyA);
		keys.add(keyB);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}

	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noKid(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keyA.remove("kid");
		keys.add(keyA);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}

	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noAlg(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keyA.remove("alg");
		keys.add(keyA);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}

	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noAlgInSecondKey(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keys.add(keyA);
		keyB.remove("alg");
		keys.add(keyB);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}


	/**
	 * Test for {@link io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noKty(){
		JsonObject server_jwks = new JsonObject();
		JsonArray keys = new JsonArray();
		keyA.remove("kty");
		keys.add(keyA);
		server_jwks.add("keys", keys);
		env.putObject("server_jwks",server_jwks);
		cond.evaluate(env);
	}
}
