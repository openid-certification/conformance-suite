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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BuildRequestObjectByValueRedirectToAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject server;

	private JsonObject authorizationEndpointRequest;

	private JsonObject alternateAuthorizationEndpointRequest;

	private JsonObject requestObjectClaims;

	private String requestObject;

	private BuildRequestObjectByValueRedirectToAuthorizationEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new BuildRequestObjectByValueRedirectToAuthorizationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Sample values from OpenID Connect Core 1.0 § 6.1

		String requestObjectClaimsString = """
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
				""";

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

		authorizationEndpointRequest = JsonParser.parseString(requestObjectClaimsString).getAsJsonObject();

		// Alternate values to test override behavior
		alternateAuthorizationEndpointRequest = JsonParser.parseString(requestObjectClaimsString).getAsJsonObject();
		alternateAuthorizationEndpointRequest.remove("state");
		alternateAuthorizationEndpointRequest.remove("nonce");
		alternateAuthorizationEndpointRequest.addProperty("state", "abcdefghijk");
		alternateAuthorizationEndpointRequest.addProperty("nonce", "0123456789ab");

		requestObjectClaims = authorizationEndpointRequest;
	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 *
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testEvaluate() throws UnsupportedEncodingException {

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.putObject("request_object_claims", requestObjectClaims);
		env.putObject("server", server);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("authorization_endpoint_request");
		verify(env, atLeastOnce()).getString("request_object");
		verify(env, atLeastOnce()).getObject("request_object_claims");
		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");

		assertThat(env.getString("redirect_to_authorization_endpoint")).startsWith("https://server.example.com/oauth/authorize");

		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("response_type")).containsExactly(UriUtils.encodeQueryParam("code id_token", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("client_id")).containsExactly(UriUtils.encodeQueryParam("s6BhdRkqt3", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("scope")).containsExactly(UriUtils.encodeQueryParam("openid", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("redirect_uri")).containsExactly(UriUtils.encodeQueryParam("https://client.example.org/cb", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("state")).isNull();
		assertThat(redirectUriParams.get("nonce")).isNull();

	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 *
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testEvaluate_override() throws UnsupportedEncodingException {

		env.putObject("authorization_endpoint_request", alternateAuthorizationEndpointRequest);
		env.putString("request_object", requestObject);
		env.putObject("request_object_claims", requestObjectClaims);
		env.putObject("server", server);

		cond.execute(env);

		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("state")).containsExactly(UriUtils.encodeQueryParam("abcdefghijk", Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("nonce")).containsExactly(UriUtils.encodeQueryParam("0123456789ab", Charset.defaultCharset().name()));

	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingAuthorizationEndpointRequest() {
		assertThrows(ConditionError.class, () -> {

			env.putString("request_object", requestObject);
			env.putObject("request_object_claims", requestObjectClaims);
			env.putObject("server", server);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingRequestObject() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
			env.putObject("request_object_claims", requestObjectClaims);
			env.putObject("server", server);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingRequestObjectClaims() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
			env.putString("request_object", requestObject);
			env.putObject("server", server);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link BuildRequestObjectByValueRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingServer() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
			env.putString("request_object", requestObject);
			env.putObject("request_object_claims", requestObjectClaims);

			cond.execute(env);

		});

	}

}
