package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ValidateClientAssertionAudClaimForPAREndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private String issuer;
	private String audienceMtls;

	private JsonObject server;
	private JsonObject serverMtls;

	private ValidateClientAssertionAudClaimForPAREndpoint cond;

	@Before
	public void setUp() throws Exception {

		cond = new ValidateClientAssertionAudClaimForPAREndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		issuer = "https://www.example.com/";

		server = JsonParser.parseString("{"
			+ "\"issuer\":\"" + issuer + "\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_issuer() {
		env.putString("client_assertion", "claims.aud", issuer);

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badIssuer() {
		env.putString("client_assertion", "claims.aud", issuer + "a");

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test
	public void testEvaluate_issuerArray() {
		var aud = new JsonArray();
		aud.add(issuer);

		var claims = new JsonObject();
		claims.add("aud", aud);
		env.putObject("client_assertion", "claims", claims);

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test
	public void testEvaluate_issuerMultiArray() {
		var aud = new JsonArray();
		aud.add(issuer);
		aud.add(issuer + "a");

		var claims = new JsonObject();
		claims.add("aud", aud);
		env.putObject("client_assertion", "claims", claims);

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badIssuerArray() {
		var aud = new JsonArray();
		aud.add(issuer + "a");

		var claims = new JsonObject();
		claims.add("aud", aud);
		env.putObject("client_assertion", "claims", claims);

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_emptyArray() {
		var aud = new JsonArray();

		var claims = new JsonObject();
		claims.add("aud", aud);
		env.putObject("client_assertion", "claims", claims);

		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "issuer");
	}

}
