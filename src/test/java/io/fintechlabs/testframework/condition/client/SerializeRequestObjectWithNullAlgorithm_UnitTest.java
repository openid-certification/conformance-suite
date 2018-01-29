package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.PlainJWT;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SerializeRequestObjectWithNullAlgorithm_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject requestObjectClaims;

	private SerializeRequestObjectWithNullAlgorithm cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new SerializeRequestObjectWithNullAlgorithm("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		requestObjectClaims = new JsonParser().parse(
				"  {\n" + 
				"   \"iss\": \"s6BhdRkqt3\",\n" + 
				"   \"aud\": \"https://server.example.com\",\n" + 
				"   \"response_type\": \"code id_token\",\n" + 
				"   \"client_id\": \"s6BhdRkqt3\",\n" + 
				"   \"redirect_uri\": \"https://client.example.org/cb\",\n" + 
				"   \"scope\": \"openid\",\n" + 
				"   \"state\": \"af0ifjsldkj\",\n" + 
				"   \"nonce\": \"n-0S6_WzA2Mj\",\n" + 
				"   \"max_age\": 86400,\n" + 
				"   \"claims\":\n" + 
				"    {\n" + 
				"     \"userinfo\":\n" + 
				"      {\n" + 
				"       \"given_name\": {\"essential\": true},\n" + 
				"       \"nickname\": null,\n" + 
				"       \"email\": {\"essential\": true},\n" + 
				"       \"email_verified\": {\"essential\": true},\n" + 
				"       \"picture\": null\n" + 
				"      },\n" + 
				"     \"id_token\":\n" + 
				"      {\n" + 
				"       \"gender\": null,\n" + 
				"       \"birthdate\": {\"essential\": true},\n" + 
				"       \"acr\": {\"values\": [\"urn:mace:incommon:iap:silver\"]}\n" + 
				"      }\n" + 
				"    }\n" + 
				"  }").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.SerializeRequestObjectWithNullAlgorithm#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 * @throws JOSEException, ParseException
	 */
	@Test
	public void testEvaluate_valuesPresent() throws JOSEException, ParseException {

		env.put("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		verify(env, atLeastOnce()).get("request_object_claims");

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		// Validate the serialized object

		PlainJWT jwt = PlainJWT.parse(requestObjectString);

		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(Algorithm.NONE);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.SerializeRequestObjectWithNullAlgorithm#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_claimsMissing() {

		cond.evaluate(env);

	}

}
