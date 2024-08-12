package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.PlainJWT;
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

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new SerializeRequestObjectWithNullAlgorithm();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		requestObjectClaims = JsonParser.parseString(
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

	/**
	 * Test method for {@link SerializeRequestObjectWithNullAlgorithm#evaluate(Environment)}.
	 *
	 * @throws JOSEException,
	 *             ParseException
	 */
	@Test
	public void testEvaluate_valuesPresent() throws JOSEException, ParseException {

		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("request_object_claims");

		String requestObjectString = env.getString("request_object");
		assertThat(requestObjectString).isNotNull();

		// Validate the serialized object

		PlainJWT jwt = PlainJWT.parse(requestObjectString);

		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(Algorithm.NONE);

	}

	/**
	 * Test method for {@link SerializeRequestObjectWithNullAlgorithm#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_claimsMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}

}
