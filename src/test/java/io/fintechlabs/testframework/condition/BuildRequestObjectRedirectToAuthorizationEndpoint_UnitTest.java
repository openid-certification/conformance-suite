package io.fintechlabs.testframework.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class BuildRequestObjectRedirectToAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private EventLog eventLog;

	private JsonObject server;

	private JsonObject authorizationEndpointRequest;

	private JsonObject alternateAuthorizationEndpointRequest;

	private JsonObject requestObjectClaims;

	private String requestObject;

	private BuildRequestObjectRedirectToAuthorizationEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new BuildRequestObjectRedirectToAuthorizationEndpoint("UNIT-TEST", eventLog, false);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		String requestObjectClaimsString =
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
				"  }";

		requestObject = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImsyYmRjIn0.ew0KICJpc3MiOiAiczZCaGRSa3" + 
				"F0MyIsDQogImF1ZCI6ICJodHRwczovL3NlcnZlci5leGFtcGxlLmNvbSIsDQogInJl" + 
				"c3BvbnNlX3R5cGUiOiAiY29kZSBpZF90b2tlbiIsDQogImNsaWVudF9pZCI6ICJzNk" + 
				"JoZFJrcXQzIiwNCiAicmVkaXJlY3RfdXJpIjogImh0dHBzOi8vY2xpZW50LmV4YW1w" + 
				"bGUub3JnL2NiIiwNCiAic2NvcGUiOiAib3BlbmlkIiwNCiAic3RhdGUiOiAiYWYwaW" + 
				"Zqc2xka2oiLA0KICJub25jZSI6ICJuLTBTNl9XekEyTWoiLA0KICJtYXhfYWdlIjog" + 
				"ODY0MDAsDQogImNsYWltcyI6IA0KICB7DQogICAidXNlcmluZm8iOiANCiAgICB7DQ" + 
				"ogICAgICJnaXZlbl9uYW1lIjogeyJlc3NlbnRpYWwiOiB0cnVlfSwNCiAgICAgIm5p" + 
				"Y2tuYW1lIjogbnVsbCwNCiAgICAgImVtYWlsIjogeyJlc3NlbnRpYWwiOiB0cnVlfS" + 
				"wNCiAgICAgImVtYWlsX3ZlcmlmaWVkIjogeyJlc3NlbnRpYWwiOiB0cnVlfSwNCiAg" + 
				"ICAgInBpY3R1cmUiOiBudWxsDQogICAgfSwNCiAgICJpZF90b2tlbiI6IA0KICAgIH" + 
				"sNCiAgICAgImdlbmRlciI6IG51bGwsDQogICAgICJiaXJ0aGRhdGUiOiB7ImVzc2Vu" + 
				"dGlhbCI6IHRydWV9LA0KICAgICAiYWNyIjogeyJ2YWx1ZXMiOiBbInVybjptYWNlOm" + 
				"luY29tbW9uOmlhcDpzaWx2ZXIiXX0NCiAgICB9DQogIH0NCn0.nwwnNsk1-Zkbmnvs" + 
				"F6zTHm8CHERFMGQPhos-EJcaH4Hh-sMgk8ePrGhw_trPYs8KQxsn6R9Emo_wHwajyF" + 
				"KzuMXZFSZ3p6Mb8dkxtVyjoy2GIzvuJT_u7PkY2t8QU9hjBcHs68PkgjDVTrG1uRTx" + 
				"0GxFbuPbj96tVuj11pTnmFCUR6IEOXKYr7iGOCRB3btfJhM0_AKQUfqKnRlrRscc8K" + 
				"ol-cSLWoYE9l5QqholImzjT_cMnNIznW9E7CDyWXTsO70xnB4SkG6pXfLSjLLlxmPG" + 
				"iyon_-Te111V8uE83IlzCYIb_NMXvtTIVc1jpspnTSD7xMbpL-2QgwUsAlMGzw";

		server = new JsonObject();
		server.addProperty("authorization_endpoint", "https://server.example.com/oauth/authorize");

		authorizationEndpointRequest = new JsonParser().parse(requestObjectClaimsString).getAsJsonObject();

		// Alternate values to test override behavior
		alternateAuthorizationEndpointRequest = new JsonParser().parse(requestObjectClaimsString).getAsJsonObject();
		alternateAuthorizationEndpointRequest.remove("state");
		alternateAuthorizationEndpointRequest.remove("nonce");
		alternateAuthorizationEndpointRequest.addProperty("state", "abcdefghijk");
		alternateAuthorizationEndpointRequest.addProperty("nonce", "0123456789ab");

		requestObjectClaims = authorizationEndpointRequest;
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testEvaluate() throws UnsupportedEncodingException {

		env.put("authorization_endpoint_request", authorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.put("request_object_claims", requestObjectClaims);
		env.put("server", server);

		cond.evaluate(env);

		verify(env, atLeastOnce()).get("authorization_endpoint_request");
		verify(env, atLeastOnce()).getString("request_object");
		verify(env, atLeastOnce()).get("request_object_claims");
		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");

		assertThat(env.getString("redirect_to_authorization_endpoint")).startsWith("https://server.example.com/oauth/authorize");

		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("response_type")).containsExactly(UriUtils.encodeQueryParam("code id_token", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("client_id")).containsExactly(UriUtils.encodeQueryParam("s6BhdRkqt3", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("scope")).containsExactly(UriUtils.encodeQueryParam("openid", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("redirect_uri")).isNull();
		assertThat(redirectUriParams.get("state")).isNull();
		assertThat(redirectUriParams.get("nonce")).isNull();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testEvaluate_override() throws UnsupportedEncodingException {

		env.put("authorization_endpoint_request", alternateAuthorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.put("request_object_claims", requestObjectClaims);
		env.put("server", server);

		cond.evaluate(env);

		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("state")).containsExactly(UriUtils.encodeQueryParam("abcdefghijk", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("nonce")).containsExactly(UriUtils.encodeQueryParam("0123456789ab", Charset.defaultCharset().name()));

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAuthorizationEndpointRequest() {

		env.putString("request_object", requestObject);
		env.put("request_object_claims", requestObjectClaims);
		env.put("server", server);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRequestObject() {

		env.put("authorization_endpoint_request", authorizationEndpointRequest);
		env.put("request_object_claims", requestObjectClaims);
		env.put("server", server);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRequestObjectClaims() {

		env.put("authorization_endpoint_request", authorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.put("server", server);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServer() {

		env.put("authorization_endpoint_request", authorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.put("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

	}

}
