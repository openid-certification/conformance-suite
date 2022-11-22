package net.openid.conformance.condition.as;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LoadRequestedIdTokenClaims_UnitTest
{

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private LoadRequestedIdTokenClaims cond;

	private JsonObject requestedClaims;

	private JsonObject essentialTrue;


	@Before
	public void setUp() throws Exception {

		cond = new LoadRequestedIdTokenClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("id_token_claims", new JsonObject());

		requestedClaims = new JsonObject();
		requestedClaims.add("email", JsonNull.INSTANCE);

		essentialTrue = new JsonObject();
		essentialTrue.addProperty("essential", true);
		requestedClaims.add("name", essentialTrue);

		env.putObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.id_token", requestedClaims);
	}


	@Test
	public void testEvaluate_noError() {

		cond.execute(env);

		assertEquals("user@example.com", env.getString("id_token_claims", "email"));
		assertEquals("Demo T. User", env.getString("id_token_claims", "name"));
		assertEquals(null, env.getElementFromObject("id_token_claims", "email_verified"));
	}

	@Test(expected = OIDFJSON.UnexpectedJsonTypeException.class)
	public void testEvaluate_essentialAsString() {
		essentialTrue.addProperty("essential", "true");

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_claimsAsString() {
		env.putString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims", "{ \"id_token\": { \"name\": { \"essential\": true } ,\"given_name\": { \"essential\": true } ,\"family_name\": { \"essential\": true } ,\"phone_number\": { \"essential\": true } ,\"email\": { \"essential\": true } ,\"address\": { \"essential\": true } ,\"birthdate\": { \"essential\": true }  } }");


		cond.execute(env);

	}

}
