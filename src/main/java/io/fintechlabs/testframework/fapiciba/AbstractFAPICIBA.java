package io.fintechlabs.testframework.fapiciba;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.CheckAuthReqIdInCallback;
import io.fintechlabs.testframework.condition.as.CheckNotificationCallbackOnlyAuthReqId;
import io.fintechlabs.testframework.condition.as.EnsureMinimumKeyLength;
import io.fintechlabs.testframework.condition.as.ValidateClientSigningKeySize;
import io.fintechlabs.testframework.condition.as.VerifyBearerTokenHeaderCallback;
import io.fintechlabs.testframework.condition.client.AddAudToRequestObject;
import io.fintechlabs.testframework.condition.client.AddAuthReqIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddCIBANotificationEndpointToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddCibaGrantTypeToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddCibaRequestSigningPS256ToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddCibaTokenDeliveryModePingToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddCibaUserCodeFalseToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddClientCredentialsGrantTypeToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddEmptyResponseTypesArrayToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddHintToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIdTokenSigningAlgPS256ToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddIssToRequestObject;
import io.fintechlabs.testframework.condition.client.AddJtiToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNbfToRequestObject;
import io.fintechlabs.testframework.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddTokenEndpointAuthMethodPrivateKeyJwtToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddTokenEndpointAuthMethodSelfSignedTlsToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CIBANotificationEndpointCalledUnexpectedly;
import io.fintechlabs.testframework.condition.client.CallAutomatedCibaApprovalEndpoint;
import io.fintechlabs.testframework.condition.client.CallBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointContentType;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus200;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfBackchannelAuthenticationEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointCacheHeaders;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus200;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus503;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatusNot200;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointRetryAfterHeaders;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CopyScopeFromDynamicRegistrationTemplateToClientConfiguration;
import io.fintechlabs.testframework.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateCIBANotificationEndpointUri;
import io.fintechlabs.testframework.condition.client.CreateDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CreateEmptyAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForCIBAGrant;
import io.fintechlabs.testframework.condition.client.EnsureErrorTokenEndpointInvalidRequest;
import io.fintechlabs.testframework.condition.client.EnsureErrorTokenEndpointSlowdownOrAuthorizationPending;
import io.fintechlabs.testframework.condition.client.EnsureMatchingFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenLength;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAuthenticationRequestIdEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAuthenticationRequestIdLength;
import io.fintechlabs.testframework.condition.client.EnsureRecommendedAuthenticationRequestIdEntropy;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.condition.client.ExpectExpiredTokenErrorFromTokenEndpoint;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromDynamicClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificatesFromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractRtHash;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FAPICIBAValidateIdTokenAuthRequestIdClaims;
import io.fintechlabs.testframework.condition.client.FAPICIBAValidateRtHash;
import io.fintechlabs.testframework.condition.client.FAPIGenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.FAPIValidateIdTokenSigningAlg;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import io.fintechlabs.testframework.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import io.fintechlabs.testframework.condition.client.SignAuthenticationRequest;
import io.fintechlabs.testframework.condition.client.TellUserToDoCIBAAuthentication;
import io.fintechlabs.testframework.condition.client.UnregisterDynamicallyRegisteredClient;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateAuthenticationRequestId;
import io.fintechlabs.testframework.condition.client.ValidateAuthenticationRequestIdExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateAuthenticationRequestIdInterval;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNotIncludeCHashAndSHash;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificates2Header;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesHeader;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInClientJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTls12;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTlsSecureCipher;
import io.fintechlabs.testframework.condition.common.FAPICheckKeyAlgInClientJWKs;
import io.fintechlabs.testframework.fapiciba.openbankinguk.OpenBankingUkPreAuthorizationStepsMTLS;
import io.fintechlabs.testframework.fapiciba.openbankinguk.OpenBankingUkPreAuthorizationStepsPrivateKeyJwt;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractFAPICIBA extends AbstractTestModule {

	private static final Logger logger = LoggerFactory.getLogger(AbstractFAPICIBA.class);
	protected enum TestType {
		PING,
		POLL
	}

	// to be used in @Variant definitions
	public static final String variant_ping_mtls = "ping-mtls";
	public static final String variant_ping_privatekeyjwt = "ping-private_key_jwt";
	public static final String variant_poll_mtls = "poll-mtls";
	public static final String variant_poll_privatekeyjwt = "poll-private_key_jwt";
	public static final String variant_openbankinguk_ping_mtls = "openbankinguk-ping-mtls";
	public static final String variant_openbankinguk_ping_privatekeyjwt = "openbankinguk-ping-private_key_jwt";
	public static final String variant_openbankinguk_poll_mtls = "openbankinguk-poll-mtls";
	public static final String variant_openbankinguk_poll_privatekeyjwt = "openbankinguk-poll-private_key_jwt";

	// for variants to fill in by calling the setup... family of methods
	private Class<? extends ConditionSequence> resourceConfiguration;
	private Class<? extends ConditionSequence> addBackchannelClientAuthentication;
	private Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Class<? extends ConditionSequence> addTokenEndpointAuthToRegistrationRequest;
	private Class<? extends ConditionSequence> additionalClientRegistrationSteps;
	private Class<? extends ConditionSequence> preAuthorizationSteps;
	private Class<? extends ConditionSequence> additionalProfileAuthorizationEndpointSetupSteps;
	private Class<? extends ConditionSequence> additionalProfileIdTokenValidationSteps;
	// this is also used to control if the test does the ping or poll behaviours for waiting for the user to
	// authenticate
	protected TestType testType;

	public static class FAPIResourceConfiguration extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		}
	}

	public static class OpenBankingUkResourceConfiguration extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToAccountsEndpoint.class);
		}
	}

	public static class PrivateKeyJwtRegistration extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(AddTokenEndpointAuthMethodPrivateKeyJwtToDynamicRegistrationRequest.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");
			callAndContinueOnFailure(AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest.class, Condition.ConditionResult.FAILURE);
		}
	}

	public static class MtlsRegistration extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(AddTokenEndpointAuthMethodSelfSignedTlsToDynamicRegistrationRequest.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		}
	}

	public static class OpenBankingUkClientRegistrationSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddClientCredentialsGrantTypeToDynamicRegistrationRequest.class, "OBRW-4.3.1");
		}
	}

	public static class OpenBankingUkProfileAuthorizationEndpointSetupSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			// Not sure there's a defined way to do these two in CIBA
//		FIXME	callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);
		}
	}

	public static class OpenBankingUkProfileIdTokenValidationSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			// FIXME: CIBA has no way to request the OB intent id...
//			callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		}
	}

	protected void addClientAuthenticationToBackchannelRequest() {
		/* This function can be inlined once all CIBA test modules are using Variants */
		call(sequence(addBackchannelClientAuthentication));
	}

	protected void addClientAuthenticationToTokenEndpointRequest() {
		/* This function can be inlined once all CIBA test modules are using Variants */
		call(sequence(addTokenEndpointClientAuthentication));
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		callAndStopOnFailure(CreateCIBANotificationEndpointUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("notification_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(CheckForKeyIdInServerJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(EnsureMinimumKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		// Set up the client configuration
		configClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		call(sequence(resourceConfiguration));

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndContinueOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class, Condition.ConditionResult.INFO);

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void configClient() {
		// Most tests just use one client
		// if any test want to use two client then override this method and config for two clients
		setupClient1();
	}

	protected void setupClient1() {
		if (env.getElementFromObject("config", "client.client_id") != null) {
			eventLog.startBlock("Verify First client: client_id supplied, assume static client configuration");
			callAndStopOnFailure(GetStaticClientConfiguration.class);
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		} else {
			eventLog.startBlock("First client: No client_id in configuration, registering client using dynamic client registration");
			callAndStopOnFailure(GetDynamicClientConfiguration.class);
			registerClient();
		}

		exposeEnvString("client_id");

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		callAndContinueOnFailure(ValidateClientSigningKeySize.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();
	}

	protected void setupClient2() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		if (env.getElementFromObject("config", "client2.client_id") != null) {
			eventLog.startBlock("Verify Second client: client_id supplied, assume static client configuration");
			callAndStopOnFailure(GetStaticClient2Configuration.class);
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		} else {
			eventLog.startBlock("Second client: No client_id in configuration, registering client using dynamic client registration");
			callAndStopOnFailure(GetDynamicClient2Configuration.class);
			registerClient();
		}

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		callAndContinueOnFailure(ValidateClientSigningKeySize.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);

		// validate the secondary MTLS keys
		callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);

		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");

		eventLog.endBlock();
	}

	public void registerClient() {

		callAndStopOnFailure(ExtractJWKsFromDynamicClientConfiguration.class);

		// create basic dynamic registration request
		callAndStopOnFailure(CreateDynamicRegistrationRequest.class);
		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

		callAndStopOnFailure(AddCibaGrantTypeToDynamicRegistrationRequest.class, "CIBA-4");
		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class, "RFC7591-2");
		callAndStopOnFailure(AddCibaUserCodeFalseToDynamicRegistrationRequest.class);

		// TODO: for now this only works for 'ping'
		callAndStopOnFailure(AddCibaTokenDeliveryModePingToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddCIBANotificationEndpointToDynamicRegistrationRequest.class, "CIBA-4");

		callAndStopOnFailure(AddCibaRequestSigningPS256ToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddIdTokenSigningAlgPS256ToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddEmptyResponseTypesArrayToDynamicRegistrationRequest.class);

		call(sequence(addTokenEndpointAuthToRegistrationRequest));

		if (additionalClientRegistrationSteps != null)
			call(sequence(additionalClientRegistrationSteps));

		callAndStopOnFailure(AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest.class);

		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		// TODO: we currently do little verification of the dynamic registration response

		// The tests expect scope to be part of the 'client' object, but it's not part of DCR so we need to manually
		// copy it across.
		callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);
	}

	public void unregisterClient1() {
		eventLog.startBlock("Unregister dynamically registered client");

		// IF management interface, delete the client to clean up
		skipIfMissing(new String[] {"client"},
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		eventLog.endBlock();
	}

	public void unregisterClient2() {
		eventLog.startBlock("Unregister dynamically registered client2");

		env.mapKey("client", "client2");

		skipIfMissing(new String[] {"client2"},
			new String[] {"registration_client_uri", "registration_access_token"},
			Condition.ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		env.unmapKey("client");

		eventLog.endBlock();
	}

	@Override
	public void start() {

		getTestExecutionManager().runInBackground(() -> {

			setStatus(Status.RUNNING);

			performAuthorizationFlow();

			return "done";
		});
	}

	protected void performPreAuthorizationSteps() {
		if (preAuthorizationSteps != null)
			call(sequence(preAuthorizationSteps));
	}

	/** Return which client is in use, for use in block identifiers */
	protected String currentClientString() {
		return "";
	}

	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class, "CIBA-7.1");
		callAndStopOnFailure(AddHintToAuthorizationEndpointRequest.class, "CIBA-7.1");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// binding_message
		// user_code

		// FIXME: this will need tweaking for OB tests; we don't need a binding message there as the
		// intent id contains sufficient context
		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequest.class, "FAPI-CIBA-5.2.2-2");

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();
	}

	protected void createAuthorizationRequestObject() {

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddIatToRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(AddExpToRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(AddNbfToRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(AddJtiToRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(AddAudToRequestObject.class, "CIBA-7.1.1");

		callAndStopOnFailure(AddIssToRequestObject.class, "CIBA-7.1.1");

	}

	protected void performValidateAuthorizationResponse() {

		callAndStopOnFailure(CheckBackchannelAuthenticationEndpointHttpStatus200.class, "CIBA-7.3");

		callAndStopOnFailure(CheckBackchannelAuthenticationEndpointContentType.class, "CIBA-7.3");

		callAndStopOnFailure(CheckIfBackchannelAuthenticationEndpointResponseError.class);

		// https://bitbucket.org/openid/mobile/issues/150/should-auth_req_id-have-limits-on
		callAndStopOnFailure(ValidateAuthenticationRequestId.class, "CIBA-7.3");

		callAndContinueOnFailure(EnsureMinimumAuthenticationRequestIdLength.class, Condition.ConditionResult.FAILURE, "CIBA-7.3");

		callAndContinueOnFailure(EnsureMinimumAuthenticationRequestIdEntropy.class, Condition.ConditionResult.FAILURE, "CIBA-7.3");

		callAndContinueOnFailure(EnsureRecommendedAuthenticationRequestIdEntropy.class, Condition.ConditionResult.WARNING, "CIBA-7.3");

		callAndContinueOnFailure(ValidateAuthenticationRequestIdExpiresIn.class, Condition.ConditionResult.FAILURE,"CIBA-7.3");

		callAndContinueOnFailure(ValidateAuthenticationRequestIdInterval.class, Condition.ConditionResult.FAILURE, "CIBA-7.3");
	}

	protected void validateErrorFromBackchannelAuthorizationRequestResponse() {

		callAndContinueOnFailure(ValidateErrorResponseFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(ValidateErrorUriFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(CheckBackchannelAuthenticationEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "CIBA-13");

	}

	protected void performPostAuthorizationResponse() {

		// Call token endpoint; 'ping' mode clients are allowed (but not required) to do this.
		// As there's no way the user could have authenticated this request, we assume we will get a
		// authorization_pending error back
		eventLog.startBlock(currentClientString() + "Call token endpoint expecting pending");
		callTokenEndpointForCibaGrant();
		verifyTokenEndpointResponseIsPendingOrSlowDown();
		eventLog.endBlock();

		long delaySeconds = 5;
		Integer interval = env.getInteger("backchannel_authentication_endpoint_response", "interval");
		if (interval != null && interval > 5) {
			// ignore intervals lower than 5; we don't want to fill the log or exhaust our retries too quickly
			delaySeconds = interval;
		}

		try {
			Thread.sleep(delaySeconds * 1000);
		} catch (InterruptedException e) {
			throw new TestFailureException(getId(), "Thread.sleep threw exception: " + e.getMessage());
		}

		// call token endpoint again and perform same checks exactly as above - but avoiding letting the request expire

		eventLog.startBlock(currentClientString() + "Call token endpoint expecting pending (second time)");
		callTokenEndpointForCibaGrant();
		verifyTokenEndpointResponseIsPendingOrSlowDown();
		eventLog.endBlock();

		String tokenEndpointError = env.getString("token_endpoint_response", "error");
		// slow_down: the interval MUST be increased by at least 5 seconds for this and all subsequent requests
		// delaySeconds is as interval
		if (!Strings.isNullOrEmpty(tokenEndpointError) && tokenEndpointError.equals("slow_down")) {
			delaySeconds = delaySeconds + 5;

			try {
				Thread.sleep(delaySeconds * 1000L);
			} catch (InterruptedException e) {
				throw new TestFailureException(getId(), "Thread.sleep threw exception: " + e.getMessage());
			}
		}

		callAutomatedEndpoint();

		waitForAuthenticationToComplete(delaySeconds);

	}

	protected void performAuthorizationRequest() {

		createAuthorizationRequestObject();

		callAndStopOnFailure(SignAuthenticationRequest.class, "CIBA-7.1.1");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		addClientAuthenticationToBackchannelRequest();

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
	}

	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		performValidateAuthorizationResponse();

		eventLog.endBlock();

		performPostAuthorizationResponse();
	}

	protected void waitForPollingAuthenticationToComplete(long delaySeconds) {
		int attempts = 0;
		while (attempts++ < 20) {
			// poll the token endpoint

			setStatus(Status.WAITING);
			try {
				Thread.sleep(delaySeconds * 1000);
			} catch (InterruptedException e) {
				throw new TestFailureException(getId(), "Thread.sleep threw exception: " + e.getMessage());
			}
			setStatus(Status.RUNNING);

			eventLog.startBlock(currentClientString() + "Polling token endpoint waiting for user to authenticate");
			callTokenEndpointForCibaGrant();
			eventLog.endBlock();
			int httpStatus = env.getInteger("token_endpoint_response_http_status");
			if (httpStatus == 200) {
				handleSuccessfulTokenEndpointResponse();
				return;
			}
			verifyTokenEndpointResponseIsPendingOrSlowDown();

			if (delaySeconds < 60) {
				delaySeconds *= 1.5;
			}
		}

		// we never moved out of pending and hence could not complete the test, test fails
		fireTestFailure();
		throw new TestFailureException(new ConditionError(getId(), "User did not authenticate before timeout"));
	}

	protected void performProfileAuthorizationEndpointSetup() {
		if (additionalProfileAuthorizationEndpointSetupSteps != null)
			call(sequence(additionalProfileAuthorizationEndpointSetupSteps));
	}

	protected void callTokenEndpointForCibaGrant() {
		callAndStopOnFailure(CreateTokenEndpointRequestForCIBAGrant.class);
		callAndStopOnFailure(AddAuthReqIdToTokenEndpointRequest.class);

		addClientAuthenticationToTokenEndpointRequest();

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
	}

	protected void verifyTokenEndpointResponseIsPendingOrSlowDown() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is pending or slow_down");

		checkStatusCode400AndValidateErrorFromTokenEndpointResponse();

		callAndStopOnFailure(EnsureErrorTokenEndpointSlowdownOrAuthorizationPending.class);

		eventLog.endBlock();
	}

	protected void verifyTokenEndpointResponseIsTokenExpired() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is expired_token");

		checkStatusCode400AndValidateErrorFromTokenEndpointResponse();

		callAndStopOnFailure(ExpectExpiredTokenErrorFromTokenEndpoint.class, "CIBA-11");

		eventLog.endBlock();
	}

	protected void verifyTokenEndpointResponseIs503Error() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is 503 error");

		callAndStopOnFailure(CheckTokenEndpointHttpStatus503.class);

		validateErrorFromTokenEndpointResponse();

		callAndStopOnFailure(CheckTokenEndpointRetryAfterHeaders.class, "CIBA-11");

		eventLog.endBlock();
	}

	protected void verifyTokenEndpointResponseIsInvalidRequest() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response is invalid_request");

		checkStatusCode400AndValidateErrorFromTokenEndpointResponse();

		callAndStopOnFailure(EnsureErrorTokenEndpointInvalidRequest.class);

		eventLog.endBlock();
	}

	protected void checkStatusCode400AndValidateErrorFromTokenEndpointResponse() {
		callAndStopOnFailure(CheckTokenEndpointHttpStatus400.class, "OIDCC-3.1.3.4");
		validateErrorFromTokenEndpointResponse();
	}

	protected void validateErrorFromTokenEndpointResponse() {
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class,"RFC6749-5.2");
	}

	protected void handleSuccessfulTokenEndpointResponse() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response");

		callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class, "RFC6749-5.1");

		callAndContinueOnFailure(CheckTokenEndpointCacheHeaders.class, Condition.ConditionResult.FAILURE, "CIBA-10.1.1", "OIDCC-3.1.3.3", "RFC6749-5.1");

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForScopesInTokenResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-15");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		performProfileIdTokenValidation();

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		// This is only required in push mode; but if the server for some reason includes it for ping/poll it shoud
		// still be correct
		call(condition(FAPICIBAValidateIdTokenAuthRequestIdClaims.class)
			.skipIfElementMissing("id_token", "claims.urn:openid:params:jwt:claim:auth_req_id")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirement("CIBA-10.3.1"));

		callAndContinueOnFailure(ValidateIdTokenNotIncludeCHashAndSHash.class, Condition.ConditionResult.WARNING);

		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		callAndContinueOnFailure(ExtractRtHash.class, Condition.ConditionResult.INFO);

		/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
		 * determined by the Extract*Hash condition
		 */
		skipIfMissing(new String[] { "rt_hash" }, null, Condition.ConditionResult.INFO,
			FAPICIBAValidateRtHash.class, Condition.ConditionResult.FAILURE, "CIBA-10.3.1", "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, null, Condition.ConditionResult.INFO,
			ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		performPostAuthorizationFlow(true);
	}

	protected void performPostAuthorizationFlow(boolean finishTest) {

		requestProtectedResource();

		if (finishTest) {
			cleanUpPingTestResources();

			fireTestFinished();
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("ciba-notification-endpoint")) {
			return handlePingCallback(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}

	}

	@UserFacing
	protected Object handlePingCallback(JsonObject requestParts) {
		getTestExecutionManager().runInBackground(() -> {

			// process the callback
			setStatus(Status.RUNNING);

			processNotificationCallback(requestParts);

			return "done";
		});

		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}

	protected void performProfileIdTokenValidation() {
		if (additionalProfileIdTokenValidationSteps != null)
			call(sequence(additionalProfileIdTokenValidationSteps));
	}

	protected void callAutomatedEndpoint() {
		env.putString("request_action", "allow");
		callAndStopOnFailure(CallAutomatedCibaApprovalEndpoint.class);
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");
	}

	protected void verifyNotificationCallback(JsonObject requestParts){
		String envKey = "notification_callback";

		eventLog.startBlock(currentClientString() + "Verify notification callback");

		env.putObject(envKey, requestParts);

		env.mapKey("client_request", envKey);

		callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-R-7.1-1");

		env.unmapKey("client_request");

		callAndStopOnFailure(VerifyBearerTokenHeaderCallback.class, "CIBA-10.2");

		callAndStopOnFailure(CheckAuthReqIdInCallback.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

		callAndStopOnFailure(CheckNotificationCallbackOnlyAuthReqId.class, "CIBA-10.2");
		eventLog.endBlock();
	}

	protected void processPingNotificationCallback(JsonObject requestParts){

		verifyNotificationCallback(requestParts);

		eventLog.startBlock(currentClientString() + "Calling token endpoint after ping notification");
		callTokenEndpointForCibaGrant();
		eventLog.endBlock();
	}

	protected void multipleCallToTokenEndpointAndVerifyResponse(){
		int attempts = 0;
		while (attempts++ < 20) {
			eventLog.startBlock(currentClientString() + "Calling token endpoint expecting one of errors of authorization_pending, slow_down, invalid_request, or 503 error");
			callTokenEndpointForCibaGrant();
			eventLog.endBlock();

			callAndContinueOnFailure(CheckTokenEndpointHttpStatusNot200.class);

			int httpStatus = env.getInteger("token_endpoint_response_http_status");
			if(httpStatus == org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503){
				verifyTokenEndpointResponseIs503Error();
				return;
			} else {
				String tokenEndpointError = env.getString("token_endpoint_response", "error");
				if ("invalid_request".equals(tokenEndpointError)) {
					verifyTokenEndpointResponseIsInvalidRequest();
					return;
				}
				verifyTokenEndpointResponseIsPendingOrSlowDown();
			}
		}
	}

	protected void waitForAuthenticationToComplete(long delaySeconds) {
		switch (testType) {
			case PING:
				// for Ping mode:
				callAndStopOnFailure(TellUserToDoCIBAAuthentication.class);

				setStatus(Status.WAITING);
				break;
			case POLL:
				waitForPollingAuthenticationToComplete(delaySeconds);
				break;
			default:
				throw new RuntimeException("unknown testType");
		}

	}

	/** called when the ping notification is received from the authorization server */
	protected void processNotificationCallback(JsonObject requestParts) {
		switch (testType) {
			case PING:
				processPingNotificationCallback(requestParts);
				handleSuccessfulTokenEndpointResponse();
				break;
			case POLL:
				callAndContinueOnFailure(CIBANotificationEndpointCalledUnexpectedly.class, Condition.ConditionResult.FAILURE);
				fireTestFinished();
			default:
				throw new RuntimeException("unknown testType");
		}
	}

	/** This should perform any actions that are specific to whichever of ping/poll/push is being tested */
	protected void modeSpecificAuthorizationEndpointRequest() {
		switch (testType) {
			case PING:
				callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");
				callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
				break;
			case POLL:
				break;
			default:
				throw new RuntimeException("unknown testType");
		}

	}

	protected void cleanupAfterBackchannelRequestShouldHaveFailed() {
		switch (testType) {

			case PING:
				pingCleanupAfterBackchannelRequestShouldHaveFailed();
				break;

			case POLL:
				pollCleanupAfterBackchannelRequestShouldHaveFailed();
				break;

			default:
				throw new RuntimeException("unknown testType");
		}
	}

	protected void pollCleanupAfterBackchannelRequestShouldHaveFailed() {
		// no cleanup necessary, just finish
		fireTestFinished();
	}

	protected void pingCleanupAfterBackchannelRequestShouldHaveFailed() {
		Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");
		if (httpStatus != org.apache.http.HttpStatus.SC_OK) {
			// error as expected, go on and complete test as normal
			cleanUpPingTestResources();
			fireTestFinished();
		} else {
			// no error - we don't want to leave a authorization request in progress (as it would result in a ping
			// notification arriving later, potentially when the user has started another test, which would be
			// confusing - complete the process
			callAutomatedEndpoint();

			setStatus(Status.WAITING);
		}
	}

	/** This should be performed before finishing test for each client to unregister dynamic client at AS*/
	protected void cleanUpPingTestResources() {
		unregisterClient1();
	}

	public void setupPingMTLS() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		addBackchannelClientAuthentication = AddMTLSClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = MtlsRegistration.class;
		testType = TestType.PING;
	}

	public void setupPingPrivateKeyJwt() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		addBackchannelClientAuthentication = AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = PrivateKeyJwtRegistration.class;
		testType = TestType.PING;
	}

	public void setupPollMTLS() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		addBackchannelClientAuthentication = AddMTLSClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = MtlsRegistration.class;
		testType = TestType.POLL;
	}

	public void setupPollPrivateKeyJwt() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		addBackchannelClientAuthentication = AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = PrivateKeyJwtRegistration.class;
		testType = TestType.POLL;
	}

	public void setupOpenBankingUkPingMTLS() {
		setupPingMTLS();
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		additionalClientRegistrationSteps = OpenBankingUkClientRegistrationSteps.class;
		preAuthorizationSteps = OpenBankingUkPreAuthorizationStepsMTLS.class;
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingUkProfileIdTokenValidationSteps.class;
	}

	public void setupOpenBankingUkPingPrivateKeyJwt() {
		setupPingPrivateKeyJwt();
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		additionalClientRegistrationSteps = OpenBankingUkClientRegistrationSteps.class;
		preAuthorizationSteps = OpenBankingUkPreAuthorizationStepsPrivateKeyJwt.class;
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingUkProfileIdTokenValidationSteps.class;
	}

	public void setupOpenBankingUkPollMTLS() {
		setupPollMTLS();
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		additionalClientRegistrationSteps = OpenBankingUkClientRegistrationSteps.class;
		preAuthorizationSteps = OpenBankingUkPreAuthorizationStepsMTLS.class;
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingUkProfileIdTokenValidationSteps.class;
	}

	public void setupOpenBankingUkPollPrivateKeyJwt() {
		setupPollPrivateKeyJwt();
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		additionalClientRegistrationSteps = OpenBankingUkClientRegistrationSteps.class;
		preAuthorizationSteps = OpenBankingUkPreAuthorizationStepsPrivateKeyJwt.class;
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingUkProfileIdTokenValidationSteps.class;
	}

}
