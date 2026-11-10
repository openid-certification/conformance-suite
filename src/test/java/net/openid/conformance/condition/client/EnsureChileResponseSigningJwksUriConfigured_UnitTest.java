package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnsureChileResponseSigningJwksUriConfigured_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private EnsureChileResponseSigningJwksUriConfigured cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putObject("config", new JsonObject());
		cond = new EnsureChileResponseSigningJwksUriConfigured();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void configure(String value) {
		JsonObject resource = new JsonObject();
		resource.addProperty("response_signing_jwks_uri", value);
		env.getObject("config").add("resource", resource);
	}

	@Test
	public void httpsUrlPasses() {
		configure("https://api.bank.example/jws-signature-validation/v1/jwks");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingFieldFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "http://api.bank.example/jwks", "not a url", "/jws-signature-validation/v1/jwks"})
	public void invalidValueFails(String value) {
		configure(value);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
