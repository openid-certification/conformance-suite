package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckServerKeysIsValid_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckServerKeysIsValid cond;

	/**
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckServerKeysIsValid();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_errorWithUnsupportedServerJWKs() {
		assertThrows(ConditionError.class, () -> {

			JsonObject serverJWKs = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"crv\":\"P-256K\","
				+ "\"x\":\"x1VOGFv0yuGvWhfQBMFZ5KPlvXwbm9HwPY-RAzZdj7g\","
				+ "\"y\":\"FhHT44a-pvfXf42c--EjrSMR7vCMtQGzrUZsItdidSs\","
				+ "\"d\":\"p8nLEvyACILbzRQYecb2bt7aSZTBuI3L39n7ygad8To\","
				+ "\"kty\":\"EC\","
				+ "\"use\":\"sig\","
				+ "\"kid\":\"PRvpagnns5AZqGSZaOAmX3BKSejLizPME_Q-KZiyz24\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("server_jwks", serverJWKs);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject serverJWKs = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"crv\":\"secp256k1\","
			+ "\"x\":\"lp8T17Y1LosMIOQmxWb7N62szWQeG-_bzb7R8e9clLI\","
			+ "\"y\":\"mXYsyG_rC8w41f9oC9XPWknFtCCpRM9iHQP7GY24MD8\","
			+ "\"kty\":\"EC\","
			+ "\"use\":\"sig\","
			+ "\"kid\":\"Rqu-16ARNH_Lgt4AtqFJDgsFlQLVOtUavMrg8Plj5U0\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("server_jwks", serverJWKs);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingServerJWKs() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

}
