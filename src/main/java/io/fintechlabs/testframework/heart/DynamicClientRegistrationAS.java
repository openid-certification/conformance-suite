package io.fintechlabs.testframework.heart;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckHeartServerJwksFields;
import io.fintechlabs.testframework.condition.client.CheckRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.EnsureAuthorizationCodeGrantTypeInClient;
import io.fintechlabs.testframework.condition.client.EnsureCodeResponseTypeInClient;
import io.fintechlabs.testframework.condition.client.EnsureDynamicRegistrationEndpointRequiresRedirectUri;
import io.fintechlabs.testframework.condition.client.EnsureImplicitGrantTypeInClient;
import io.fintechlabs.testframework.condition.client.EnsureTokenResponseTypeInClient;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.SetDynamicRegistrationRequestGrantTypeToAuthorizationCode;
import io.fintechlabs.testframework.condition.client.SetDynamicRegistrationRequestGrantTypeToImplicit;
import io.fintechlabs.testframework.condition.client.UnregisterDynamicallyRegisteredClient;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckHeartServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.SetTLSTestHostFromConfig;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

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

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS10.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS11.class, "HEART-OAuth2-6");

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		callAndContinueOnFailure(GetDynamicServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckForKeyIdInServerJWKs.class, "OIDCC-10.1");

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
		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

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
		skipIfMissing(null,
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		callAndStopOnFailure(EnsureImplicitGrantTypeInClient.class);
		callAndStopOnFailure(EnsureTokenResponseTypeInClient.class);

		callAndStopOnFailure(SetDynamicRegistrationRequestGrantTypeToAuthorizationCode.class);
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		// IF management interface, delete the client to clean up
		skipIfMissing(null,
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		// client is still in the env, check the grant_types and response_types
		callAndStopOnFailure(EnsureAuthorizationCodeGrantTypeInClient.class);
		callAndStopOnFailure(EnsureCodeResponseTypeInClient.class);

		fireTestFinished();
	}
}
