package net.openid.conformance.fapiciba;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckAuthReqIdInCallback;
import net.openid.conformance.condition.as.CheckCIBAModeIsPoll;
import net.openid.conformance.condition.as.CheckNotificationCallbackOnlyAuthReqId;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.FAPIBrazilSetPaymentDateToToday;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.VerifyBearerTokenHeaderCallback;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddAuthReqIdToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddCIBANotificationEndpointToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddCibaGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddCibaRequestSigningPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePingToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePollToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddCibaUserCodeFalseToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientCredentialsGrantTypeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientNameToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddClientX509CertificateClaimToPublicJWKs;
import net.openid.conformance.condition.client.AddEmptyResponseTypesArrayToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddHintToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdTokenSigningAlgPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.AddJtiToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.AddScopeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodPrivateKeyJwtToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthMethodSelfSignedTlsToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddTokenEndpointAuthSigningAlgPS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CIBANotificationEndpointCalledUnexpectedly;
import net.openid.conformance.condition.client.CallAutomatedCibaApprovalEndpoint;
import net.openid.conformance.condition.client.CallBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckBackchannelAuthenticationEndpointContentType;
import net.openid.conformance.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromBackchannelAuthenticationEndpointContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckIfBackchannelAuthenticationEndpointResponseError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckIncomingContentTypeIsApplicationJson;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.CheckTokenEndpointCacheHeaders;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus503;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusNot200;
import net.openid.conformance.condition.client.CheckTokenEndpointRetryAfterHeaders;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CopyScopeFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import net.openid.conformance.condition.client.CreateCIBANotificationEndpointUri;
import net.openid.conformance.condition.client.CreateEmptyAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateEmptyDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.CreateRandomClientNotificationToken;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForCIBAGrant;
import net.openid.conformance.condition.client.EnsureAccessTokenValuesAreDifferent;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureErrorTokenEndpointInvalidRequest;
import net.openid.conformance.condition.client.EnsureErrorTokenEndpointSlowdownOrAuthorizationPending;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumAuthenticationRequestIdEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthenticationRequestIdLength;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.EnsureRecommendedAuthenticationRequestIdEntropy;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
import net.openid.conformance.condition.client.ExpectExpiredTokenErrorFromTokenEndpoint;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAtHash;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractInitialAccessTokenFromStoredConfig;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractRtHash;
import net.openid.conformance.condition.client.ExtractSignedJwtFromResourceResponse;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenExp;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseTyp;
import net.openid.conformance.condition.client.FAPICIBAValidateIdTokenAuthRequestIdClaims;
import net.openid.conformance.condition.client.FAPICIBAValidateRtHash;
import net.openid.conformance.condition.client.FAPIValidateIdTokenEncryptionAlg;
import net.openid.conformance.condition.client.FAPIValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FetchFreshIdTokenHintIfHintValueIsNotConfigured;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateMTLSCertificateFromJWKs;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetLoginHintToConsentIdIfHintValueIsNotConfigured;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.SignRequestObjectIncludeMediaType;
import net.openid.conformance.condition.client.StoreOriginalClient2Configuration;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.TellUserToDoCIBAAuthentication;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateAuthenticationRequestId;
import net.openid.conformance.condition.client.ValidateAuthenticationRequestIdExpiresIn;
import net.openid.conformance.condition.client.ValidateAuthenticationRequestIdInterval;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.ValidateIdTokenNotIncludeCHashAndSHash;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateResourceResponseJwtClaims;
import net.openid.conformance.condition.client.ValidateResourceResponseSignature;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckCIBAServerConfiguration;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.common.FAPICheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	CIBAMode.class,
	ClientRegistration.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {
	"consumerdataright_au", "openinsurance_brazil", "openbanking_ksa"
})
@VariantNotApplicable(parameter = CIBAMode.class, values = { "push" })
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.client_id",
	"client2.client_id"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name",
	"client2.client_name",
	"client.initial_access_token",
	"client2.initial_access_token"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"client.acr_value",
	"consent.productType",
	"resource.consentUrl",
	"resource.brazilCpf",
	"resource.brazilCnpj",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
	"directory.keystore"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil", configurationFields = {
		"client.org_jwks",
		"client.acr_value",
		"consent.productType",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilOrganizationId",
		"directory.keystore"
})
@VariantHidesConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.jwks",
	"mtls.cert",
	"mtls.key",
	"mtls.ca",
	"client2.jwks",
	"mtls2.cert",
	"mtls2.key",
	"mtls2.ca"
})
public abstract class AbstractFAPICIBAID1 extends AbstractTestModule {

	// for variants to fill in by calling the setup... family of methods
	private Class<? extends ConditionSequence> resourceConfiguration;
	private Supplier<? extends ConditionSequence> addBackchannelClientAuthentication;
	protected Class<? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Class<? extends ConditionSequence> addTokenEndpointAuthToRegistrationRequest;
	private Class<? extends ConditionSequence> additionalClientRegistrationSteps;
	private Supplier<? extends ConditionSequence> preAuthorizationSteps;
	private Class<? extends ConditionSequence> additionalProfileAuthorizationEndpointSetupSteps;
	private Class<? extends ConditionSequence> additionalProfileIdTokenValidationSteps;
	private Class<? extends ConditionSequence> supportMTLSEndpointAliases;
	// this is also used to control if the test does the ping or poll behaviours for waiting for the user to
	// authenticate
	protected CIBAMode testType;
	private boolean brazilPayments;
	protected boolean isBrazil() {
		return brazilPayments;
	}

	public void setAddBackchannelClientAuthentication(Supplier<? extends ConditionSequence> addBackchannelClientAuthentication) {
		this.addBackchannelClientAuthentication = addBackchannelClientAuthentication;
	}

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

	public static class OpenBankingBrazilProfileAuthorizationEndpointSetupSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			// In order to address the chicken-and-problem of requiring id_token_hint, the RP tests will expose
			// a /token/obtain endpoint to fetch a recently signed id token for the given environment.
			// If there is an id_token_hint configured in the hint_value, then it will be used.
			// If not, and if there is a config `client.obtain_id_token` containing a URL to the /token/obtain endpoint,
			// then a fresh id token will be fetched and used as the id_token_hint value.
			callAndStopOnFailure(FetchFreshIdTokenHintIfHintValueIsNotConfigured.class);
			// If a login_hint has been configured in the hint_value field in the configuration,
			// it will be used as the login_hint. If not, then we take the Brazil consent_id and use that as login_hint.
			callAndStopOnFailure(SetLoginHintToConsentIdIfHintValueIsNotConfigured.class);
		}
	}

	public static class PlainFapiProfileIdTokenValidationSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6", "FAPI1-ADV-8.6");
		}
	}

	public static class OpenBankingUkProfileIdTokenValidationSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			// FIXME: CIBA has no way to request the OB intent id...
//			callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
			callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6", "FAPI1-ADV-8.6");
		}
	}

	public static class OpenBankingBrazilProfileIdTokenValidationSteps extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-1");
			callAndContinueOnFailure(FAPIBrazilValidateIdTokenExp.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
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
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		testType = getVariant(CIBAMode.class);
		env.putString("ciba_mode", testType.name());

		callAndStopOnFailure(CreateCIBANotificationEndpointUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("notification_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
			FAPI1FinalOPProfile profile = getVariant(FAPI1FinalOPProfile.class);
			ClientAuthType authType = getVariant(ClientAuthType.class);
			if (authType != ClientAuthType.MTLS && profile != FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				// we only need to call the mtls aliased backchannel authentication endpoint when using mtls client auth
				// (but need to use the mtls alias for the token endpoint whenever we're using certificate bound
				// access tokens)
				env.removeNativeValue("backchannel_authentication_endpoint");
			}
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckCIBAServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE,"OIDCC-10.1");
		callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
		callAndContinueOnFailure(FAPIEnsureMinimumServerKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		// Set up the client configuration
		configClient();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		call(sequence(resourceConfiguration));

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndContinueOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class, Condition.ConditionResult.INFO);

		brazilPayments = scopeContains("payments");
		if(isBrazil()) {
			callAndStopOnFailure(CheckCIBAModeIsPoll.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
		}

		onConfigure();

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected boolean scopeContains(String requiredScope) {
		String scope = env.getString("config", "client.scope");
		if (Strings.isNullOrEmpty(scope)) {
			throw new TestFailureException(getId(), "'scope' seems to be missing from client configuration");
		}
		List<String> scopes = Arrays.asList(scope.split(" "));
		return scopes.contains(requiredScope);
	}

	protected void onConfigure() {
	}

	protected void configClient() {
		// Most tests just use one client
		// if any test want to use two client then override this method and config for two clients
		setupClient1();
	}

	protected void setupClient1() {
		switch (getVariant(ClientRegistration.class)) {
		case STATIC_CLIENT:
			eventLog.startBlock("Verify First client: static client configuration");
			callAndStopOnFailure(GetStaticClientConfiguration.class);
			callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
			break;
		case DYNAMIC_CLIENT:
			eventLog.startBlock("First client: registering client using dynamic client registration");
			callAndStopOnFailure(StoreOriginalClientConfiguration.class);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			callAndStopOnFailure(ExtractInitialAccessTokenFromStoredConfig.class);
			registerClient();
			break;
		}

		exposeEnvString("client_id");

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		callAndContinueOnFailure(FAPIEnsureMinimumClientKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();
	}

	protected void setupClient2() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		switch (getVariant(ClientRegistration.class)) {
		case STATIC_CLIENT:
			eventLog.startBlock("Verify Second client: static client configuration");
			callAndStopOnFailure(GetStaticClient2Configuration.class);
			callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);
			break;
		case DYNAMIC_CLIENT:
			eventLog.startBlock("Second client: registering client using dynamic client registration");
			callAndStopOnFailure(StoreOriginalClient2Configuration.class);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			callAndStopOnFailure(ExtractInitialAccessTokenFromStoredConfig.class);
			registerClient();
			break;
		}

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		callAndContinueOnFailure(FAPIEnsureMinimumClientKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		// validate the secondary MTLS keys
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);

		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");

		eventLog.endBlock();
	}

	public void registerClient() {

		callAndStopOnFailure(GeneratePS256ClientJWKsWithKeyID.class);
		callAndStopOnFailure(GenerateMTLSCertificateFromJWKs.class);
		callAndStopOnFailure(AddClientX509CertificateClaimToPublicJWKs.class);

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientNameToDynamicRegistrationRequest.class);
		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

		callAndStopOnFailure(AddCibaGrantTypeToDynamicRegistrationRequest.class, "CIBA-4");
		callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class, "RFC7591-2");
		callAndStopOnFailure(AddCibaUserCodeFalseToDynamicRegistrationRequest.class);

		switch (testType) {
		case PING:
			callAndStopOnFailure(AddCibaTokenDeliveryModePingToDynamicRegistrationRequest.class);
			callAndStopOnFailure(AddCIBANotificationEndpointToDynamicRegistrationRequest.class, "CIBA-4");
			break;
		case POLL:
			callAndStopOnFailure(AddCibaTokenDeliveryModePollToDynamicRegistrationRequest.class);
			break;
		default:
			throw new RuntimeException("unknown testType");
		}

		callAndStopOnFailure(AddCibaRequestSigningPS256ToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddIdTokenSigningAlgPS256ToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddEmptyResponseTypesArrayToDynamicRegistrationRequest.class);

		performProfileClientRegistrationSetup();

		call(sequence(addTokenEndpointAuthToRegistrationRequest));

		if (additionalClientRegistrationSteps != null) {
			call(sequence(additionalClientRegistrationSteps));
		}

		callAndStopOnFailure(AddTLSBoundAccessTokensTrueToDynamicRegistrationRequest.class);

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));

		// TODO: we currently do little verification of the dynamic registration response

		// The tests expect scope to be part of the 'client' object, but it's not part of DCR so we need to manually
		// copy it across.
		callAndStopOnFailure(CopyScopeFromDynamicRegistrationTemplateToClientConfiguration.class);
		if (env.isKeyMapped("client")) {
			// Add acr_values to client2 only
			callAndStopOnFailure(CopyAcrValueFromDynamicRegistrationTemplateToClientConfiguration.class);
		}
	}

	protected void performProfileClientRegistrationSetup() {
		// Nothing more to do by default
	}

	public void unregisterClient1() {
		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {

			eventLog.startBlock("Unregister dynamically registered client");

			// IF management interface, delete the client to clean up
			call(condition(UnregisterDynamicallyRegisteredClient.class)
				.skipIfObjectsMissing(new String[]{"client"})
				.onSkip(Condition.ConditionResult.INFO)
				.onFail(Condition.ConditionResult.WARNING)
				.dontStopOnFailure());

			eventLog.endBlock();
		}
	}

	public void unregisterClient2() {
		if (getVariant(ClientRegistration.class) == ClientRegistration.DYNAMIC_CLIENT) {

			eventLog.startBlock("Unregister dynamically registered client2");

			env.mapKey("client", "client2");

			call(condition(UnregisterDynamicallyRegisteredClient.class)
				.skipIfObjectsMissing(new String[]{"client"})
				.onSkip(Condition.ConditionResult.INFO)
				.onFail(Condition.ConditionResult.WARNING)
				.dontStopOnFailure());

			env.unmapKey("client");

			eventLog.endBlock();
		}
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
		if (preAuthorizationSteps != null) {
			call(sequence(preAuthorizationSteps));
		}
	}

	/** Return which client is in use, for use in block identifiers */
	protected String currentClientString() {
		return "";
	}

	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		performProfileAuthorizationEndpointSetup();

		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class, "CIBA-7.1");
		callAndStopOnFailure(AddHintToAuthorizationEndpointRequest.class, "CIBA-7.1");


		// The spec also defines these parameters that we don't currently set:
		// binding_message
		// user_code

		// FIXME: this will need tweaking for OB tests; we don't need a binding message there as the
		// intent id contains sufficient context
		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequest.class, "FAPI-CIBA-5.2.2-2");

		modeSpecificAuthorizationEndpointRequest();
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

		callAndContinueOnFailure(CheckErrorDescriptionFromBackchannelAuthenticationEndpointContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");

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

		if (isSecondClient()) {
			callAndStopOnFailure(SignRequestObjectIncludeMediaType.class, "CIBA-7.1.1", "JAR-4");
		}
		else {
			callAndStopOnFailure(SignRequestObject.class, "CIBA-7.1.1");
		}

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
		for (int attempts = 0; attempts < 20; attempts++) {
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
				delaySeconds = Math.round(delaySeconds * 1.5);
			}
		}

		// we never moved out of pending and hence could not complete the test, test fails
		throw new TestFailureException(getId(), "User did not authenticate before timeout");
	}

	protected void performProfileAuthorizationEndpointSetup() {
		if (additionalProfileAuthorizationEndpointSetupSteps != null) {
			call(sequence(additionalProfileAuthorizationEndpointSetupSteps));
		}
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
		callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class,"RFC6749-5.2");
		callAndStopOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class,"RFC6749-5.2");
	}

	protected void handleSuccessfulTokenEndpointResponse() {
		eventLog.startBlock(currentClientString() + "Verify token endpoint response");

		callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class, "RFC6749-5.1");

		callAndContinueOnFailure(CheckTokenEndpointCacheHeaders.class, Condition.ConditionResult.FAILURE, "CIBA-10.1.1", "OIDCC-3.1.3.3", "RFC6749-5.1");

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14", "FAPI1-BASE-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, Condition.ConditionResult.WARNING, "CIBA-10.1.1", "RFC6749-5.1");
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
		if (isBrazil()) {
			skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
				FAPIBrazilValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-13");
		}

		callAndContinueOnFailure(CheckForRefreshTokenValue.class, Condition.ConditionResult.INFO);

		// TODO: Required for Brazil?
		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16", "FAPI1-BASE-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16", "FAPI1-BASE-5.2.2-16");

		skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
			ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2.1-6", "FAPI1-BASE-5.2.2.1-6", "OIDCC-3.3.2.5");

		call(new PerformStandardIdTokenChecks());

		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		performProfileIdTokenValidation();

		skipIfElementMissing("id_token", "jwe_header", Condition.ConditionResult.INFO,
			FAPIValidateIdTokenEncryptionAlg.class, Condition.ConditionResult.FAILURE,"FAPI-RW-8.6.1-1");

		// This is only required in push mode; but if the server for some reason includes it for ping/poll it should still be correct
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

		eventLog.endBlock();

		performPostAuthorizationFlow(true);
	}

	protected void performPostAuthorizationFlow(boolean finishTest) {

		requestProtectedResource();

		if (finishTest) {
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
		if (additionalProfileIdTokenValidationSteps != null) {
			call(sequence(additionalProfileIdTokenValidationSteps));
		}
	}

	protected void callAutomatedEndpoint() {
		env.putString("request_action", "allow");
		callAndStopOnFailure(CallAutomatedCibaApprovalEndpoint.class);
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		if (!isSecondClient()) {
			// these are optional; only add them for the first client
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);
		}

		if (getVariant(FAPI1FinalOPProfile.class) == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			if (isBrazil()) {
				// setup to call the payments initiation API, which requires a signed jwt request body
				call(sequenceOf(condition(CreateIdempotencyKey.class), condition(AddIdempotencyKeyHeader.class)));
				callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetResourceMethodToPost.class);
				callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);
				callAndStopOnFailure(AddEndToEndIdToPaymentRequestEntityClaims.class);

				// we reuse the request object conditions to add various jwt claims; it would perhaps make sense to make
				// these more generic.
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

				// aud (in the JWT request): the Resource Provider (eg the institution holding the account) must validate if the value of the aud field matches the endpoint being triggered;
				callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

				//iss (in the JWT request and in the JWT response): the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender;
				callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

				//jti (in the JWT request and in the JWT response): the value of the jti field shall be filled with the UUID defined by the institution according to [RFC4122] version 4;
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

				//iat (in the JWT request and in the JWT response): the iat field shall be filled with the message generation time and according to the standard established in [RFC7519](https:// datatracker.ietf.org/doc/html/rfc7519#section-2) to the NumericDate format.
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

				call(exec().unmapKey("request_object_claims"));

				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		}

		callAndStopOnFailure(CallProtectedResource.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));

		if (isBrazil()) {

			ConditionSequence sequence =
				new RefreshTokenRequestSteps(isSecondClient(), addTokenEndpointClientAuthentication)
					.skip(EnsureAccessTokenValuesAreDifferent.class, "");

			int httpStatus = env.getInteger("endpoint_response", "status");
			for(int i = 0; i < 3 && httpStatus == 401; i++) {
				call(sequence);
				callAndStopOnFailure(CallProtectedResource.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");
				httpStatus = env.getInteger("endpoint_response", "status");
			}
		}

		Optional<ConditionSequence> statusCheckingSequence = getBrazilPaymentsStatusCodeCheck();
		call(statusCheckingSequence.orElse(
			sequenceOf(condition(EnsureHttpStatusCodeIs200or201.class).onFail(Condition.ConditionResult.FAILURE))
		));
		call(exec().unmapKey("endpoint_response"));

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-10");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		if (!isSecondClient()) {
			callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");
		}

		if (isBrazil()) {
			validateBrazilPaymentInitiationSignedResponse();
		} else {
			callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");
		}

		eventLog.endBlock();
	}

	protected void validateBrazilPaymentInitiationSignedResponse() {
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseTyp.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		// signature needs to be validated against the organisation jwks (already fetched during pre-auth steps)

		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));
		callAndStopOnFailure(FetchServerKeys.class);
		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		callAndContinueOnFailure(ValidateResourceResponseSignature.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));
	}

	protected void verifyNotificationCallback(JsonObject requestParts){
		String envKey = "notification_callback";

		eventLog.startBlock(currentClientString() + "Verify notification callback");

		env.putObject(envKey, requestParts);

		env.mapKey("client_request", envKey);

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");
		callAndStopOnFailure(CheckIncomingContentTypeIsApplicationJson.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");

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
		for (int attempts = 0; attempts < 20; attempts++) {
			eventLog.startBlock(currentClientString() + "Calling token endpoint expecting one of errors of authorization_pending, slow_down, invalid_request, or 503 error");
			callTokenEndpointForCibaGrant();
			eventLog.endBlock();

			callAndContinueOnFailure(CheckTokenEndpointHttpStatusNot200.class, Condition.ConditionResult.INFO);

			int httpStatus = env.getInteger("token_endpoint_response_http_status");
			if(httpStatus == HttpStatus.SERVICE_UNAVAILABLE.value()){
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
				break;
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
		if (httpStatus != org.apache.hc.core5.http.HttpStatus.SC_OK) {
			// error as expected, go on and complete test as normal
			fireTestFinished();
		} else {
			// no error - we don't want to leave a authorization request in progress (as it would result in a ping
			// notification arriving later, potentially when the user has started another test, which would be
			// confusing - complete the process
			callAutomatedEndpoint();

			setStatus(Status.WAITING);
		}
	}

	boolean isSecondClient() {
		return env.isKeyMapped("client");
	}

	@Override
	public void cleanup() {
		unregisterClient1();
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addBackchannelClientAuthentication = () -> new AddMTLSClientAuthenticationToBackchannelRequest();
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = MtlsRegistration.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addBackchannelClientAuthentication = () -> new AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(isSecondClient(), true);
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
		addTokenEndpointAuthToRegistrationRequest = PrivateKeyJwtRegistration.class;
		// FAPI requires the use of MTLS sender constrained access tokens, so we must use the MTLS version of the
		// token endpoint even when using private_key_jwt client authentication
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		additionalClientRegistrationSteps = null;
		preAuthorizationSteps = null;
		additionalProfileAuthorizationEndpointSetupSteps = null;
		additionalProfileIdTokenValidationSteps = PlainFapiProfileIdTokenValidationSteps.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		resourceConfiguration = OpenBankingUkResourceConfiguration.class;
		additionalClientRegistrationSteps = OpenBankingUkClientRegistrationSteps.class;
		preAuthorizationSteps = () -> new OpenBankingUkPreAuthorizationSteps(isSecondClient(), addTokenEndpointClientAuthentication);
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingUkProfileIdTokenValidationSteps.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		additionalClientRegistrationSteps = null;
		preAuthorizationSteps = () -> createBrazilPreauthSteps();
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingBrazilProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingBrazilProfileIdTokenValidationSteps.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil")
	public void setupOpenInsuranceBrazil() {
		resourceConfiguration = FAPIResourceConfiguration.class;
		additionalClientRegistrationSteps = null;
		preAuthorizationSteps = () -> createBrazilPreauthSteps();
		additionalProfileAuthorizationEndpointSetupSteps = OpenBankingBrazilProfileAuthorizationEndpointSetupSteps.class;
		additionalProfileIdTokenValidationSteps = OpenBankingBrazilProfileIdTokenValidationSteps.class;
	}
	protected ConditionSequence createBrazilPreauthSteps() {
		boolean isSecondClient = isSecondClient();
		boolean isDpop = false;
		boolean isBrazilOpenInsurance = false;
		boolean stopAfterConsentEndpoint = false;

		if (isBrazil()) {
			eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
			updatePaymentConsent();
		}
		return new OpenBankingBrazilPreAuthorizationSteps(
			isSecondClient, isDpop, addTokenEndpointClientAuthentication, isBrazil(), isBrazilOpenInsurance, stopAfterConsentEndpoint, false
		);
	}

	protected void updatePaymentConsent() {
		callAndStopOnFailure(FAPIBrazilSetPaymentDateToToday.class);
	}

	/**
	 * Subclasses may have more complex needs for this, so let them provide it as a sequence
	 * @return
	 */
	protected Optional<ConditionSequence> getBrazilPaymentsStatusCodeCheck() {
		return Optional.empty();
	}
}
