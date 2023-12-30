package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidateIdTokenNotIncludeCHashAndSHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenNotIncludeCHashAndSHash cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateIdTokenNotIncludeCHashAndSHash();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvalutate_isGood() {
		// Define claims object
		JsonObject claims = JsonParser.parseString("""
				{
				  "at_hash": "SzqRJ7WtQMjkoDyPMXnpvA",
				  "sub": "1001",
				  "aud": "21541757519",
				  "auth_time": 1553590905,
				  "iss": "https://fapidev-as.authlete.net/",
				  "exp": 1553591207,
				  "iat": 1553590907
				}""").getAsJsonObject();

		JsonObject o = new JsonObject();
		o.add("claims", claims);

		env.putObject("id_token", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvalutate_isBad() {
		// Define claims object
		JsonObject claims = JsonParser.parseString("""
				{
				  "at_hash": "SzqRJ7WtQMjkoDyPMXnpvA",
				  "sub": "1001",
				  "aud": "21541757519",
				  "auth_time": 1553590905,
				  "iss": "https://fapidev-as.authlete.net/",
				  "exp": 1553591207,
				  "iat": 1553590907,
				  "s_hash": "1553590907"
				}""").getAsJsonObject();

		JsonObject o = new JsonObject();
		o.add("claims", claims);

		env.putObject("id_token", o);

		cond.execute(env);
	}

}
