package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaimsTest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims cond;

	private JsonObject request;


	private void addRequestClaims(Environment env, JsonObject claims) {
		env.putObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, claims);
	}

	private void addUserinfoClaims(Environment env, JsonObject claims) {
		env.putObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.userinfo", claims);
	}

	@BeforeEach
	public void setUp() {

		cond = new AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		String requestStr =
				"""
						    {
						      "client_id": "52480754053",
						      "redirect_uri": "https://example.com:8443/test/a/oidf-fapi2-op-test/callback",
						      "scope": "openid",
						      "response_type": "code",
						      "code_challenge": "5RXriS_3zf4ASe_cX4LqerLGvNN1rGfUu5_EsqE99lg",
						      "code_challenge_method": "S256",
						      "claims": {
						        "id_token": {
						          "name": null,
						          "given_name": {},
						          "family_name": {
						            "essential": true
						          },
						          "email": {
						            "7EimPyJ0oq": "eRpxS9SF3u"
						          },
						          "birthdate": {
						            "essential": false
						          },
						          "phone_number": null,
						          "address": {}
						        }
						      }
						    }\
						""";

		JsonObject requestObj = (JsonObject) JsonParser.parseString(requestStr);
		addRequestClaims(env, requestObj);
	}


	@Test
	public void testEvaluate_noUserInfo() {
		addRequestClaims(env, request);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_userinfoClaims() {
		assertThrows(ConditionError.class, () -> {
			String userinfoRequestStr =
				"""
						{
						  "name": null,
						  "given_name": {},
						  "family_name": {
						    "essential": true
						  },
						  "email": {
						    "7G53NBuCSL": "jR8hZ8SOcr"
						  },
						  "birthdate": {
						    "essential": false
						  },
						  "phone_number": null,
						  "address": {}
						}""";
			JsonObject userinfoRequestObj = (JsonObject) JsonParser.parseString(userinfoRequestStr);
			addRequestClaims(env, request);
			addUserinfoClaims(env, userinfoRequestObj);
			cond.execute(env);
		});
	}


	@Test
	public void testEvaluate_blankUserinfoClaims() {
		String userinfoRequestStr =
			"""
			{
			}\
			""";
		JsonObject userinfoRequestObj = (JsonObject) JsonParser.parseString(userinfoRequestStr);
		addRequestClaims(env,request);
		addUserinfoClaims(env, userinfoRequestObj);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonStandardUserinfoclaims() {
		String userinfoRequestStr =
			"{\"foo\": null }";
		JsonObject userinfoRequestObj = (JsonObject) JsonParser.parseString(userinfoRequestStr);
		addRequestClaims(env,request);
		addUserinfoClaims(env, userinfoRequestObj);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_singleUserinfoclaim() {
		assertThrows(ConditionError.class, () -> {
			String userinfoRequestStr =
				"""
						{
						  "name": null
						}""";
			JsonObject userinfoRequestObj = (JsonObject) JsonParser.parseString(userinfoRequestStr);
			addRequestClaims(env, request);
			addUserinfoClaims(env, userinfoRequestObj);
			cond.execute(env);
		});
	}
}
