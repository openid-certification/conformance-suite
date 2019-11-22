package net.openid.conformance.heart;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckHeartServerJwksFields;
import net.openid.conformance.condition.client.CheckRedirectUri;
import net.openid.conformance.condition.client.CreateDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.EnsureAuthorizationCodeGrantTypeInClient;
import net.openid.conformance.condition.client.EnsureCodeResponseTypeInClient;
import net.openid.conformance.condition.client.EnsureDynamicRegistrationEndpointRequiresRedirectUri;
import net.openid.conformance.condition.client.EnsureImplicitGrantTypeInClient;
import net.openid.conformance.condition.client.EnsureTokenResponseTypeInClient;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicClientConfiguration;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.SetDynamicRegistrationRequestGrantTypeToAuthorizationCode;
import net.openid.conformance.condition.client.SetDynamicRegistrationRequestGrantTypeToImplicit;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckHeartServerConfiguration;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12;
import net.openid.conformance.condition.common.SetTLSTestHostFromConfig;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;

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
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, Condition.ConditionResult.WARNING, "RFC7517-4.5");

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
