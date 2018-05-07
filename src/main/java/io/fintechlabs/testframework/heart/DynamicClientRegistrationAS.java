package io.fintechlabs.testframework.heart;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInJWKs;
import io.fintechlabs.testframework.condition.common.CheckHeartServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.SetTLSTestHostFromConfig;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author srmoore
 *
 */
@PublishTestModule(
	testName = "heart-dynamic-client-registration",
	displayName = "HEART AS: OAuth Dynamic Client Registration",
	profile = "HEART",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_name",
		"tls.testHost",
		"tls.testPort"
	}
)
public class DynamicClientRegistrationAS extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(DynamicClientRegistrationAS.class);

	public DynamicClientRegistrationAS(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "HEART-OAuth2-6");
		call(DisallowTLS10.class, "HEART-OAuth2-6");
		call(DisallowTLS11.class, "HEART-OAuth2-6");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		call(GetDynamicServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckForKeyIdInJWKs.class, "OIDCC-10.1");

		// get the client configuration that we'll use to dynamically register
		callAndStopOnFailure(GetDynamicClientConfiguration.class);

		callAndStopOnFailure(CheckRedirectUri.class);

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		// create basic dynamic registration request
		callAndStopOnFailure(CreateDynamicRegistrationRequest.class);
		expose("client_name", env.findElement("dynamic_registration_request", "client_name").getAsString());

		// Run without redirect uris OAuth 2.0 Dynamic Registration section 2.
		callAndStopOnFailure(SetDynamicRegistrationRequestGrantTypeToImplicit.class);
		callAndStopOnFailure(EnsureDynamicRegistrationEndpointRequiresRedirectUri.class);
		callAndStopOnFailure(SetDynamicRegistrationRequestGrantTypeToAuthorizationCode.class);
		callAndStopOnFailure(EnsureDynamicRegistrationEndpointRequiresRedirectUri.class);

		// Add in the redirect URIs needed for proper registration
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		callAndStopOnFailure(SetDynamicRegistrationRequestGrantTypeToImplicit.class);
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		// IF management interface, delete the client to clean up
		skipIfMissing(new String[] {},
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		callAndStopOnFailure(EnsureImplicitGrantTypeInClient.class);
		callAndStopOnFailure(EnsureTokenResponseTypeInClient.class);

		callAndStopOnFailure(SetDynamicRegistrationRequestGrantTypeToAuthorizationCode.class);
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		// IF management interface, delete the client to clean up
		skipIfMissing(new String[] {},
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		// client is still in the env, check the grant_types and response_types
		callAndStopOnFailure(EnsureAuthorizationCodeGrantTypeInClient.class);
		callAndStopOnFailure(EnsureCodeResponseTypeInClient.class);

		fireTestFinished();
		stop();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		logIncomingHttpRequest(path, requestParts);

		// dispatch based on the path
		// since we're only doing the registration, nothing should come back on the callback

		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);

	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}
}
