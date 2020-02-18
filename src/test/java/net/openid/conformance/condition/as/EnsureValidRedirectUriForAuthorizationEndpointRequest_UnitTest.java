package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
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

import static org.junit.Assert.*;
@RunWith(MockitoJUnitRunner.class)
public class EnsureValidRedirectUriForAuthorizationEndpointRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureValidRedirectUriForAuthorizationEndpointRequest cond;

	@Before
	public void setUp() throws Exception {

		cond = new EnsureValidRedirectUriForAuthorizationEndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noErrorHttps() {
		JsonObject client = new JsonObject();
		JsonArray redirectUris = new JsonArray();
		redirectUris.add("https://www.certification.openid.net/redirect/uri1");
		redirectUris.add("https://www.certification.openid.net/redirect/uri2");
		client.add("redirect_uris", redirectUris);
		client.addProperty("application_type", "web");
		env.putObject("client", client);
		JsonObject effectiveRequest = new JsonObject();
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI,
								"https://www.certification.openid.net/redirect/uri1");
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, "code id_token");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveRequest);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorCodeWithHttp() {
		JsonObject client = new JsonObject();
		JsonArray redirectUris = new JsonArray();
		redirectUris.add("https://www.certification.openid.net/redirect/uri1");
		redirectUris.add("http://www.certification.openid.net/code");
		client.add("redirect_uris", redirectUris);
		env.putObject("client", client);
		JsonObject effectiveRequest = new JsonObject();
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI,
			"http://www.certification.openid.net/code");
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, "code");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveRequest);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorNativeWithHttpLocalhost() {
		JsonObject client = new JsonObject();
		JsonArray redirectUris = new JsonArray();
		redirectUris.add("https://localhost/redirect/uri1");
		redirectUris.add("http://localhost/native");
		client.add("redirect_uris", redirectUris);
		client.addProperty("application_type", "native");
		env.putObject("client", client);
		JsonObject effectiveRequest = new JsonObject();
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI,
			"http://localhost/native");
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, "code id_token");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveRequest);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_httpWhenNotCode() {
		JsonObject client = new JsonObject();
		JsonArray redirectUris = new JsonArray();
		redirectUris.add("https://www.certification.openid.net/redirect/uri1");
		redirectUris.add("http://www.certification.openid.net/redirect/uri2");
		client.add("redirect_uris", redirectUris);
		env.putObject("client", client);
		client.addProperty("application_type", "web");
		JsonObject effectiveRequest = new JsonObject();
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI,
			"http://www.certification.openid.net/redirect/uri2");
		effectiveRequest.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, "code id_token");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveRequest);
		cond.execute(env);
	}


}
