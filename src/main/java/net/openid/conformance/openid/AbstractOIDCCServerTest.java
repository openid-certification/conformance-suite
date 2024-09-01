package net.openid.conformance.openid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddFormBasedClientIdAuthenticationParameters;
import net.openid.conformance.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckCallbackContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsPost;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientAuthNone;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientSecretBasic;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsClientSecretPost;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsMTLS;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import net.openid.conformance.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractInitialAccessTokenFromStoredConfig;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateJWKsFromClientSecret;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToFormPost;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeFromEnvironment;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToUserInfoEndpoint;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromAuthorizationResponseEncryption;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.ValidateIssIfPresentInAuthorizationResponse;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.VerifyIdTokenSubConsistentHybridFlow;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.function.Supplier;

@VariantParameters({
	ServerMetadata.class,
	ClientAuthType.class,
	ResponseType.class,
	ResponseMode.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.issuer",
	"server.jwks_uri",
	"server.authorization_endpoint",
	"server.token_endpoint",
	"server.userinfo_endpoint"
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
	"server.discoveryUrl"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name",
	"client.initial_access_token"
})
@VariantHidesConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_secret",
	"client.jwks",
	"client2.client_secret",
	"client2.jwks"
})
@VariantHidesConfigurationFields(parameter = ResponseType.class, value = "id_token", configurationFields = {
	"server.token_endpoint"
	/* we don't exclude "server.userinfo_endpoint" as this would prevent it appearing in the 'implicit' configuration
	* form - see comment in TestPlanModuleWithVariant's constructor */
})
@VariantHidesConfigurationFields(parameter = ResponseType.class, value = "id_token token", configurationFields = {
	"server.token_endpoint"
})
public abstract class AbstractOIDCCServerTest extends AbstractRedirectServerTestModule {

	protected ResponseType responseType;
	protected boolean formPost;
	private boolean serverSupportsDiscovery;

	protected Class<? extends ConditionSequence> profileStaticClientConfiguration;
	protected Supplier<? extends ConditionSequence> profileCompleteClientConfiguration;
	protected Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Class<? extends ConditionSequence> supportMTLSEndpointAliases;

	public static class ConfigureClientForClientSecretJwt extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(GenerateJWKsFromClientSecret.class);
		}
	}

	public class ConfigureClientForAuthTypeNone extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			if (serverSupportsDiscovery) {
				callAndContinueOnFailure(EnsureServerConfigurationSupportsClientAuthNone.class, ConditionResult.FAILURE);
			}
		}
	}

	public class ConfigureClientForClientSecretBasic extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			if (serverSupportsDiscovery) {
				callAndContinueOnFailure(EnsureServerConfigurationSupportsClientSecretBasic.class, ConditionResult.FAILURE);
			}
		}
	}

	public class ConfigureClientForClientSecretPost extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			if (serverSupportsDiscovery) {
				callAndContinueOnFailure(EnsureServerConfigurationSupportsClientSecretPost.class, ConditionResult.FAILURE);
			}
		}
	}

	public class ConfigureClientForPrivateKeyJwt extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			if (serverSupportsDiscovery) {
				callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, ConditionResult.FAILURE);
			}
		}
	}

	public static class ConfigureClientForMtls extends AbstractConditionSequence {
		private boolean serverSupportsDiscovery;
		private boolean secondClient;

		public ConfigureClientForMtls(boolean serverSupportsDiscovery, boolean secondClient) {
			this.secondClient = secondClient;
			this.serverSupportsDiscovery = serverSupportsDiscovery;
		}

		@Override
		public void evaluate() {
			if (!secondClient) {
				if (serverSupportsDiscovery) {
					callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, ConditionResult.FAILURE);
				}
				callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
				callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
			} else {
				// TODO: use environment mapping so we don't need two versions of these conditions
				callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
				callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);
			}
			callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class ConfigureStaticClientForPrivateKeyJwt extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		}
	}

	public static class ConfigureStaticClient extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// for auth types other than private_key_jwt we might still need a jwks if the server is returning
			// encrypted id_tokens; extract one if it's there.
			call(condition(ValidateClientJWKsPrivatePart.class)
				.skipIfElementMissing("client", "jwks")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC7517-1.1")
				.onFail(Condition.ConditionResult.FAILURE));

			call(condition(ExtractJWKsFromStaticClientConfiguration.class)
				.skipIfElementMissing("client", "jwks")
				.onSkip(Condition.ConditionResult.INFO)
				.onFail(Condition.ConditionResult.FAILURE));

			call(condition(CheckDistinctKeyIdValueInClientJWKs.class)
				.skipIfElementMissing("client", "jwks")
				.onSkip(Condition.ConditionResult.INFO)
				.requirements("RFC7517-4.5")
				.onFail(Condition.ConditionResult.FAILURE));
		}
	}

	public static class AddAuthClientNoneAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddFormBasedClientIdAuthenticationParameters.class);
		}
	}

	public static class AddBasicAuthClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);
		}
	}

	public static class AddFormBasedClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		}
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "none")
	public void setupNone() {
		profileStaticClientConfiguration = ConfigureStaticClient.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForAuthTypeNone();
		addTokenEndpointClientAuthentication = AddAuthClientNoneAuthenticationToTokenRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		profileStaticClientConfiguration = ConfigureStaticClient.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForClientSecretBasic();
		addTokenEndpointClientAuthentication = AddBasicAuthClientSecretAuthenticationToTokenRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		profileStaticClientConfiguration = ConfigureStaticClient.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForClientSecretPost();
		addTokenEndpointClientAuthentication = AddFormBasedClientSecretAuthenticationToTokenRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		profileStaticClientConfiguration = ConfigureStaticClient.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForClientSecretJwt();
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		profileStaticClientConfiguration = ConfigureStaticClientForPrivateKeyJwt.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForPrivateKeyJwt();
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMtls() {
		profileStaticClientConfiguration = ConfigureStaticClient.class;
		profileCompleteClientConfiguration = () -> new ConfigureClientForMtls(serverSupportsDiscovery(), isSecondClient());
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}
		formPost = getVariant(ResponseMode.class) == ResponseMode.FORM_POST;
		serverSupportsDiscovery = getVariant(ServerMetadata.class) == ServerMetadata.DISCOVERY;

		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);
		env.putString("client_auth_type", clientAuthType.toString());

		responseType = getVariant(ResponseType.class);
		env.putString("response_type", responseType.toString());

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		switch (getVariant(ServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(GetDynamicServerConfiguration.class);
				break;
			case STATIC:
				callAndStopOnFailure(GetStaticServerConfiguration.class);
				break;
		}

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
		// Includes verify-base64url and bare-keys assertions (OIDC test)
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");

		skipTestIfSigningAlgorithmNotSupported();

		// Set up the client configuration
		configureClient();

		skipTestIfScopesNotSupported();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(SetProtectedResourceUrlToUserInfoEndpoint.class);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void skipTestIfSigningAlgorithmNotSupported() {
		// Just apply for 'oidcc-idtoken-unsigned' test
	}

	protected void skipTestIfScopesNotSupported() {
		// Just apply for scope tests
	}

	protected void skipTestIfNoneUnsupported() {
		JsonElement el = env.getElementFromObject("server", "request_object_signing_alg_values_supported");
		if (el != null && el.isJsonArray()) {
			JsonArray serverValues = el.getAsJsonArray();
			if (!serverValues.contains(new JsonPrimitive("none"))) {
				fireTestSkipped("'none' is not listed in request_object_signing_alg_values_supported - assuming it is not supported.");
			}
		}
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	protected void configureClient() {
		// Set up the client configuration
		switch (getVariant(ClientRegistration.class)) {
		case STATIC_CLIENT:
			callAndStopOnFailure(GetStaticClientConfiguration.class);
			configureStaticClient();
			break;
		case DYNAMIC_CLIENT:
			callAndStopOnFailure(StoreOriginalClientConfiguration.class);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			callAndStopOnFailure(ExtractInitialAccessTokenFromStoredConfig.class);
			configureDynamicClient();
			break;
		}

		exposeEnvString("client_id");

		completeClientConfiguration();
	}

	protected void configureStaticClient() {
		if (profileStaticClientConfiguration != null) {
			call(sequence(profileStaticClientConfiguration));
		}
	}

	protected void createDynamicClientRegistrationRequest() {

		// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_Dynamic
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType));

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));
	}

	protected void configureDynamicClient() {

		createDynamicClientRegistrationRequest();

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
	}

	protected void completeClientConfiguration() {
		callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);

		if (profileCompleteClientConfiguration != null) {
			call(sequence(profileCompleteClientConfiguration));
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performAuthorizationFlow();
	}

	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		performRedirect();
		eventLog.endBlock();
	}

	public static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {
		protected boolean formPost;

		CreateAuthorizationRequestSteps(boolean formPost) {
			this.formPost = formPost;
		}
		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			callAndStopOnFailure(CreateRandomStateValue.class);
			call(exec().exposeEnvironmentString("state"));
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(CreateRandomNonceValue.class);
			call(exec().exposeEnvironmentString("nonce"));
			callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeFromEnvironment.class);

			if (formPost) {
				callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToFormPost.class);
			}
		}
	}

	protected void createAuthorizationRequest() {
		call(createAuthorizationRequestSequence());
	}

	protected ConditionSequence createAuthorizationRequestSequence() {
		return new CreateAuthorizationRequestSteps(formPost);
	}

	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (formPost) {
			env.mapKey("authorization_endpoint_response", "callback_body_form_params");
			callAndContinueOnFailure(CheckCallbackHttpMethodIsPost.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(CheckCallbackContentTypeIsFormUrlEncoded.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		} else if (isCodeFlow()) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		} else {
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		}

		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE);
		if (responseType.includesCode()) {
			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		}
		if (responseType.includesToken()) {
			callAndStopOnFailure(ExtractAccessTokenFromAuthorizationResponse.class);
		}
		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		if (responseType.includesIdToken()) {
			skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
				ValidateIdTokenFromAuthorizationResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class);

			// save the id_token returned from the authorization endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

			performAuthorizationEndpointIdTokenValidation();
		}
		if (responseType.includesCode()) {
			performAuthorizationCodeValidation();
		}
		if (responseType.includesToken()) {
			requestProtectedResource();
		}
		performPostAuthorizationFlow();
	}

	protected void performAuthorizationEndpointIdTokenValidation() {
		performIdTokenValidation();
	}

	protected void performIdTokenValidation() {
		call(new PerformStandardIdTokenChecks());
	}

	protected void performAuthorizationCodeValidation() {
	}

	protected void performPostAuthorizationFlow() {
		if (responseType.includesCode()) {
			// call the token endpoint and complete the flow
			createAuthorizationCodeRequest();
			requestAuthorizationCode();
			requestProtectedResource();
		}
		onPostAuthorizationFlowComplete();
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		if (addTokenEndpointClientAuthentication != null) {
			call(sequence(addTokenEndpointClientAuthentication));
		}
	}

	protected void requestAuthorizationCode() {
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class, ConditionResult.INFO);

		skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
			ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.1.3.3", "OIDCC-3.3.3.3");

		// save the id_token returned from the token endpoint
		env.putObject("token_endpoint_id_token", env.getObject("id_token"));

		additionalTokenEndpointResponseValidation();

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyIdTokenSubConsistentHybridFlow.class, ConditionResult.FAILURE, "OIDCC-2");
		}
	}

	protected void additionalTokenEndpointResponseValidation() {
		performIdTokenValidation();
	}

	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResource.class);
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		eventLog.endBlock();
	}

	/**
	 * Do generic checks on an error response from the authorization endpoint
	 *
	 * Generally called from onAuthorizationCallbackResponse. The caller stills needs to check for the exact specific
	 * error code their test scenario expects.
	 */
	protected void performGenericAuthorizationEndpointErrorResponseValidation() {
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(RejectAuthCodeInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB.class, ConditionResult.WARNING, "RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorDescriptionFromAuthorizationEndpointResponseError.class, ConditionResult.FAILURE,"RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorUriFromAuthorizationEndpointResponseError.class, ConditionResult.FAILURE,"RFC6749-4.1.2.1");
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	@Override
	public void cleanup() {
		unregisterClient();
	}

	public void unregisterClient() {
		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {
			eventLog.startBlock(currentClientString() + "Unregister dynamically registered client");

			call(condition(UnregisterDynamicallyRegisteredClient.class)
				.skipIfObjectsMissing(new String[]{"client"})
				.onSkip(ConditionResult.INFO)
				.onFail(ConditionResult.WARNING)
				.dontStopOnFailure());

			eventLog.endBlock();
		}
	}

	protected String currentClientString() {
		return "";
	}

	protected boolean isSecondClient() {
		return false;
	}

	protected boolean serverSupportsDiscovery() {
		return serverSupportsDiscovery;
	}

	protected boolean isCodeFlow() {
		return responseType.equals(ResponseType.CODE);
	}

	protected boolean isHybridFlow() {
		return responseType.includesCode() && !isCodeFlow();
	}

	protected boolean isImplicitFlow() {
		return !responseType.includesCode();
	}
}
