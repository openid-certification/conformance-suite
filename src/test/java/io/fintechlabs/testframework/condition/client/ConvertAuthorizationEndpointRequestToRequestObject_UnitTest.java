package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ConvertAuthorizationEndpointRequestToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject authorizationEndpointRequest;

	private ConvertAuthorizationEndpointRequestToRequestObject cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ConvertAuthorizationEndpointRequestToRequestObject("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		authorizationEndpointRequest = new JsonParser().parse(
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
				"  }")
			.getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.put("authorization_endpoint_request", authorizationEndpointRequest);

		cond.evaluate(env);

		verify(env, atLeastOnce()).get("authorization_endpoint_request");
		assertThat(env.get("request_object_claims")).isEqualTo(authorizationEndpointRequest);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

}
