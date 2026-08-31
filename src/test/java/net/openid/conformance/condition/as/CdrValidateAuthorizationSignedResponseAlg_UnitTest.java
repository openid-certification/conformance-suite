package net.openid.conformance.condition.as;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateAuthorizationSignedResponseAlg_UnitTest {

	// public keys only
	private static final String RSA_KEY = "{\"kty\":\"RSA\",%s\"use\":\"sig\",\"kid\":\"k1\","
		+ "\"n\":\"jGjAX7zVDvKPHmuvnPreAmvB7rQZfEUW3lUW-qC8eDhhm4IDLQdd-oPnZocsHrbCMkGSU45pvXMiUnAmqAIQvkfsCVMB4C4zx4b9cCakEd6-p0mzs2Ib972eNlPvGgtVoK-2fT2eOICyfnBIQPXZlDp211kbhHNRUiPjirYqogsbwqIyW6QAghhCitAUXvj0nkuFpai-cbEyF6cbmXGH4CJyLQPgeYlKAcU-eOR6bTKO0OtF66TvJ6MMzoNSTvW0S6ADFLos00zNBPycfsGyzitkVWv6IkFAQNyc9GkAB3fnAJyPY-4bTBcYPHrDT8FLBQxWLNJDmIY0lQXlbg7CGQ\",\"e\":\"AQAB\"}";

	private static final String EC_ES256_KEY = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"alg\":\"ES256\",\"use\":\"sig\",\"kid\":\"k2\","
		+ "\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\"}";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateAuthorizationSignedResponseAlg cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateAuthorizationSignedResponseAlg();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addServerJwksWithKey(String key) {
		env.putObject("server_jwks", JsonParser.parseString("{\"keys\":[" + key + "]}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_ps256IsGood() {
		addServerJwksWithKey(RSA_KEY.formatted("\"alg\":\"PS256\","));
		cond.execute(env);
	}

	@Test
	public void testEvaluate_es256IsGood() {
		addServerJwksWithKey(EC_ES256_KEY);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_rs256NotPermitted() {
		assertThrows(ConditionError.class, () -> {
			addServerJwksWithKey(RSA_KEY.formatted("\"alg\":\"RS256\","));
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_keyWithoutAlgFails() {
		assertThrows(ConditionError.class, () -> {
			addServerJwksWithKey(RSA_KEY.formatted(""));
			cond.execute(env);
		});
	}

}
