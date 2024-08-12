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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ConvertAuthorizationEndpointRequestToRequestObject();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		authorizationEndpointRequest = JsonParser.parseString(
						"""
								  {
								   "iss": "s6BhdRkqt3",
								   "aud": "https://server.example.com",
								   "response_type": "code id_token",
								   "client_id": "s6BhdRkqt3",
								   "redirect_uri": "https://client.example.org/cb",
								   "scope": "openid",
								   "state": "af0ifjsldkj",
								   "nonce": "n-0S6_WzA2Mj",
								   "max_age": 86400,
								   "claims":
								    {
								     "userinfo":
								      {
								       "given_name": {"essential": true},
								       "nickname": null,
								       "email": {"essential": true},
								       "email_verified": {"essential": true},
								       "picture": null
								      },
								     "id_token":
								      {
								       "gender": null,
								       "birthdate": {"essential": true},
								       "acr": {"values": ["urn:mace:incommon:iap:silver"]}
								      }
								    }
								  }\
								""")
			.getAsJsonObject();

	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("authorization_endpoint_request");
		assertThat(env.getObject("request_object_claims")).isEqualTo(authorizationEndpointRequest);
	}

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}

}
