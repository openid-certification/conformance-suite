package net.openid.conformance.fapi2spfinal;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.FAPI2FinalEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPI2FinalEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIBrazilEncryptRequestObject;
import net.openid.conformance.condition.as.FAPIBrazilSetPaymentDateToToday;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddClientIdToRequestObject;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectByValueRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.condition.client.BuildUnsignedPAREndpointRequest;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CallPAREndpointAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceAllowingDpopNonceError;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckForPARResponseExpiresIn;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForRequestUriValue;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.condition.client.CheckServerKeysIsValid;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.EnsureMinimumRequestUriEntropy;
import net.openid.conformance.condition.client.EnsureNoRefreshTokenInTokenResponse;
import net.openid.conformance.condition.client.ExpectNoIdTokenInTokenResponse;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAtHash;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromJARMResponse;
import net.openid.conformance.condition.client.ExtractCHash;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJARMFromURLQuery;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.ExtractSHash;
import net.openid.conformance.condition.client.ExtractSignedJwtFromResourceResponse;
import net.openid.conformance.condition.client.FAPI2ValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.FAPI2ValidateJarmSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseTyp;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetOauthDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlFragment;
import net.openid.conformance.condition.client.RejectErrorInUrlFragment;
import net.openid.conformance.condition.client.RejectNonJarmResponsesInUrlQuery;
import net.openid.conformance.condition.client.RejectStateInUrlFragmentForCodeFlow;
import net.openid.conformance.condition.client.RequireIssInAuthorizationResponse;
import net.openid.conformance.condition.client.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.SignRequestObjectIncludeMediaType;
import net.openid.conformance.condition.client.ValidateAtHash;
import net.openid.conformance.condition.client.ValidateCHash;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateClientPrivateKeysAreDifferent;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.ValidateJARMEncryptionAlg;
import net.openid.conformance.condition.client.ValidateJARMEncryptionEnc;
import net.openid.conformance.condition.client.ValidateJARMExpRecommendations;
import net.openid.conformance.condition.client.ValidateJARMFromURLQueryEncryption;
import net.openid.conformance.condition.client.ValidateJARMResponse;
import net.openid.conformance.condition.client.ValidateJARMSignatureUsingKid;
import net.openid.conformance.condition.client.ValidateJARMSigningAlg;
import net.openid.conformance.condition.client.ValidateMTLSCertificates2Header;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.condition.client.ValidateResourceResponseJwtClaims;
import net.openid.conformance.condition.client.ValidateResourceResponseSignature;
import net.openid.conformance.condition.client.ValidateSHash;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.ValidateSuccessfulJARMResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.common.CheckClientCredentialsOnlyServerConfiguration;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.FAPI2CheckKeyAlgInClientJWKs;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToPAREndpointRequest;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VCIAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthToEndpointRequest;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@VariantParameters({
	ClientAuthType.class,
	FAPI2AuthRequestMethod.class,
	FAPIOpenIDConnect.class,
	FAPI2SenderConstrainMethod.class,
	FAPI2FinalOPProfile.class,
	FAPIOpenIDConnect.class,
	FAPIResponseMode.class,
	AuthorizationRequestType.class,
	VCIProfile.class,
	VCIGrantType.class,
	VCIAuthorizationCodeFlowVariant.class,
	VCI1FinalCredentialFormat.class,
	VCICredentialEncryption.class,
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "plain_fapi", configurationFields = {
	"resource.resourceMethod",
	"resource.resourceMediaType",
	"resource.resourceRequestBody"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "fapi_client_credentials_grant", configurationFields = {
	"resource.resourceMethod",
	"resource.resourceMediaType",
	"resource.resourceRequestBody"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "openbanking_uk", configurationFields = {
	"resource.resourceUrlAccountRequests",
	"resource.resourceUrlAccountsResource"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "consumerdataright_au", configurationFields = {
	"resource.cdrVersion"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"consent.productType",
	"resource.consentUrl",
	"resource.brazilCpf",
	"resource.brazilCnpj",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment",
	"directory.keystore"
})
@VariantConfigurationFields(parameter = FAPI2SenderConstrainMethod.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = FAPI2SenderConstrainMethod.class, value = "dpop", configurationFields = {
	"client.dpop_signing_alg",
	"client2.dpop_signing_alg",
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = {"client_attestation"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_attestation", configurationFields = {
	"vci.client_attester_keys_jwks",
	"vci.client_attestation_issuer"
})
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "client_attestation", configurationFields = {
	"client.jwks"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
// mtls fields are only necessary for non-plain_fapi profiles, but we can't use @VariantHidesConfigurationFields
// as that would result in them not being shown for mtls client auth
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "openbanking_uk", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "consumerdataright_au", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "connectid_au", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "cbuae", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
@VariantHidesConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "connectid_au", configurationFields = {
	"resource.resourceUrl", // the userinfo endpoint is always used
	"client.scope", // scope is always openid
	"client2.scope"
})
@VariantConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "vci", configurationFields = {
	"vci.credential_issuer_url",
	"vci.credential_configuration_id",
	"vci.credential_proof_type_hint",
	"vci.key_attestation_jwks",
	"vci.authorization_server",
})
@VariantHidesConfigurationFields(parameter = FAPI2FinalOPProfile.class, value = "vci", configurationFields = {
	"resource.resourceUrl", // credential endpoint comes from VCI metadata
	"resource.resourceMethod",
	"resource.resourceMediaType",
	"resource.resourceRequestBody",
})
// Hide VCI variant parameters from non-VCI profiles
@VariantNotApplicableWhen(parameter = VCIProfile.class, values = {"plain_vci", "haip"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
@VariantNotApplicableWhen(parameter = VCIGrantType.class, values = {"authorization_code", "pre_authorization_code"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
@VariantNotApplicableWhen(parameter = VCIAuthorizationCodeFlowVariant.class, values = {"wallet_initiated", "issuer_initiated"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
@VariantNotApplicableWhen(parameter = VCI1FinalCredentialFormat.class, values = {"sd_jwt_vc", "mdoc"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
@VariantNotApplicableWhen(parameter = VCICredentialEncryption.class, values = {"plain", "encrypted"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"plain_fapi", "openbanking_uk", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "fapi_client_credentials_grant"})
// Restrict JARM and OpenID Connect for VCI
@VariantNotApplicableWhen(parameter = FAPIResponseMode.class, values = {"jarm"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"vci"})
@VariantNotApplicableWhen(parameter = FAPIOpenIDConnect.class, values = {"openid_connect"},
	whenParameter = FAPI2FinalOPProfile.class, hasValues = {"vci"})
// VCI-specific variant configuration
@VariantConfigurationFields(parameter = VCIGrantType.class, value = "pre_authorization_code", configurationFields = {"vci.static_tx_code"})
@VariantHidesConfigurationFields(parameter = VCIAuthorizationCodeFlowVariant.class, value = "wallet_initiated", configurationFields = {
	"vci.credential_offer_endpoint"
})
@VariantHidesConfigurationFields(parameter = VCIProfile.class, value = "haip", configurationFields = {"client.dpop_signing_alg", "client2.dpop_signing_alg"})
// Restrict RAR and pre-auth code for HAIP
@VariantNotApplicableWhen(parameter = AuthorizationRequestType.class, values = {"rar"},
	whenParameter = VCIProfile.class, hasValues = {"haip"})
@VariantNotApplicableWhen(parameter = VCIGrantType.class, values = {"pre_authorization_code"},
	whenParameter = VCIProfile.class, hasValues = {"haip"})
@VariantConfigurationFields(parameter = AuthorizationRequestType.class, value = "rar", configurationFields = {
	"resource.richAuthorizationRequest",
})
public abstract class AbstractFAPI2SPFinalServerTestModule extends AbstractRedirectServerTestModule {

	protected int whichClient;
	protected Boolean jarm;
	protected boolean allowPlainErrorResponseForJarm = false;
	protected Boolean isPar;
	protected Boolean isBrazil;
	protected Boolean isOpenId;
	protected Boolean isSignedRequest;
	protected Boolean brazilPayments; // whether using Brazil payments APIs
	protected Boolean profileRequiresMtlsEverywhere;
	protected Boolean useDpopAuthCodeBinding;
	protected Boolean isRarRequest;
	protected Boolean clientCredentailsGrant;

	protected FAPI2ProfileBehavior profileBehavior;

	// for variants to fill in by calling the setup... family of methods
	private Class <? extends ConditionSequence> resourceConfiguration;
	protected Class <? extends ConditionSequence> addTokenEndpointClientAuthentication;
	private Supplier <? extends ConditionSequence> preAuthorizationSteps;
	protected Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;
	private Class <? extends ConditionSequence> profileIdTokenValidationSteps;
	private Class <? extends ConditionSequence> supportMTLSEndpointAliases;
	protected Class <? extends ConditionSequence> addParEndpointClientAuthentication;
	protected Supplier <? extends ConditionSequence> createDpopForParEndpointSteps;
	protected Supplier <? extends ConditionSequence> createDpopForTokenEndpointSteps;
	protected Supplier <? extends ConditionSequence> createDpopForResourceEndpointSteps;

	public static class FAPIResourceConfiguration extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		}
	}

	public static class OpenBankingUkResourceConfiguration extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(SetProtectedResourceUrlToAccountsEndpoint.class);
		}
	}

	protected Boolean isDpop() {
		return getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.DPOP;
	}

	protected Boolean isMTLS() {
		return getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
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

		if (getVariant(FAPIOpenIDConnect.class) == FAPIOpenIDConnect.PLAIN_OAUTH && scopeContains("openid")) {
			throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
		}

		jarm = getVariant(FAPIResponseMode.class) == FAPIResponseMode.JARM;
		isPar = true;
		isBrazil = getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.OPENBANKING_BRAZIL;
		isOpenId = getVariant(FAPIOpenIDConnect.class) == FAPIOpenIDConnect.OPENID_CONNECT;
		isSignedRequest = getVariant(FAPI2AuthRequestMethod.class) == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION;
		isRarRequest = getVariant(AuthorizationRequestType.class) == AuthorizationRequestType.RAR;
		clientCredentailsGrant = profileBehavior.isClientCredentialsGrantOnly();
		useDpopAuthCodeBinding = false;

		profileRequiresMtlsEverywhere = profileBehavior.requiresMtlsEverywhere();
		if (! clientCredentailsGrant) {
			callAndStopOnFailure(CreateRedirectUri.class);

			// this is inserted by the create call above, expose it to the test environment for publication
			exposeEnvString("redirect_uri");
		}

		// Fetch authorization server configuration (profile may override, e.g. VCI)
		profileBehavior.fetchServerConfiguration(this);

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		if (clientCredentailsGrant) {
			callAndStopOnFailure(CheckClientCredentialsOnlyServerConfiguration.class);
		}
		else {
			callAndStopOnFailure(CheckServerConfiguration.class);
		}

		if (isOpenId || jarm) {
			callAndStopOnFailure(FetchServerKeys.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
			callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
			callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");
			callAndContinueOnFailure(FAPI2FinalEnsureMinimumServerKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3");
		}

		if (isRarRequest) {
			profileBehavior.configureRAR(this);
		}

		// Profile-specific additional configuration (e.g., VCI credential metadata resolution)
		profileBehavior.configureAdditional(this);

		whichClient = 1;

		// Set up the client configuration
		configureClient();
		setupResourceEndpoint();

		// Profile-specific client attestation configuration (e.g., VCI attestation keys)
		profileBehavior.configureClientAttestation(this);

		brazilPayments = isBrazil && scopeContains("payments");

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void setupResourceEndpoint() {
		profileBehavior.setupResourceEndpoint(this);
	}

	/**
	 * Default server configuration fetch: standard OIDC or OAuth discovery.
	 * Called by profile behaviors that don't override server config fetching.
	 */
	public void defaultFetchServerConfiguration() {
		if (isOpenId) {
			callAndStopOnFailure(GetDynamicServerConfiguration.class);
		} else {
			callAndStopOnFailure(GetOauthDynamicServerConfiguration.class);
		}
	}

	/**
	 * Default resource endpoint setup: GetResourceEndpointConfiguration + resourceConfiguration sequence.
	 * Called by profile behaviors that don't override resource endpoint setup.
	 */
	public void defaultSetupResourceEndpoint() {
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		call(sequence(resourceConfiguration));
	}

	/**
	 * Default client JWKs validation: ValidateClientJWKsPrivatePart.
	 * Called by profile behaviors that don't override client JWKs validation.
	 */
	public void defaultValidateClientJwks() {
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
	}

	/**
	 * Default key algorithm validation: FAPI2 key alg check.
	 * Called by profile behaviors that don't override key algorithm validation.
	 */
	public void defaultValidateKeyAlgorithms() {
		callAndContinueOnFailure(FAPI2CheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
	}

	/**
	 * Default id_token signing algorithm validation: FAPI2 signing alg check.
	 * Called by profile behaviors that don't override id_token signing alg validation.
	 */
	public void defaultValidateIdTokenSigningAlg() {
		callAndContinueOnFailure(FAPI2ValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4");
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		// Profile-specific client configuration (e.g., VCI generates JWKs if missing)
		profileBehavior.configureClient(this);

		exposeEnvString("client_id");

		boolean mtlsRequired =
			getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS ||
			getVariant(ClientAuthType.class) == ClientAuthType.MTLS ||
			profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		}

		validateClientConfiguration();
	}

	protected void configureSecondClient() {
		eventLog.startBlock("Verify configuration of second client");

		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);

		boolean mtlsRequired =
			getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS ||
			getVariant(ClientAuthType.class) == ClientAuthType.MTLS ||
			profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);
		}

		validateClientConfiguration();

		unmapClient();

		callAndContinueOnFailure(ValidateClientPrivateKeysAreDifferent.class, ConditionResult.FAILURE);

		eventLog.endBlock();
	}

	protected void validateClientConfiguration() {
		profileBehavior.configureClientScope(this);
		profileBehavior.validateClientJwks(this);
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		profileBehavior.validateKeyAlgorithms(this);
		callAndContinueOnFailure(FAPI2FinalEnsureMinimumClientKeyLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3");

		boolean mtlsRequired =
			getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS ||
			getVariant(ClientAuthType.class) == ClientAuthType.MTLS ||
			profileRequiresMtlsEverywhere;

		if (mtlsRequired) {
			callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);
		}
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		if (clientCredentailsGrant) {
			performCredentialsFlow();
		}
		else {
			performAuthorizationFlow();
		}
	}

	protected void performPreAuthorizationSteps() {
		if (preAuthorizationSteps != null) {
			call(sequence(preAuthorizationSteps));
		}
	}

	protected void performCredentialsFlow() {
		performPostAuthorizationFlow();
	}

	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Create authorization request");

		createAuthorizationRequest();

		if (isSignedRequest) {
			createAuthorizationRequestObject();
		} else {
			// the request object is implicitly created by the PAR endpoint, but
			// AbstractBuildRequestObjectRedirectToAuthorizationEndpoint needs to know what is in the implicit
			// request object.
			env.mapKey("request_object_claims", "pushed_authorization_request_form_parameters");
		}

		if (isPar) {
			eventLog.startBlock(currentClientString() + "Make request to PAR endpoint");
			if (isSignedRequest) {
				callAndStopOnFailure(BuildRequestObjectPostToPAREndpoint.class);
			} else {
				callAndStopOnFailure(BuildUnsignedPAREndpointRequest.class);
			}

			if (env.getObject("pushed_authorization_request_endpoint_request_headers") == null) {
				env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
			}
			env.mapKey("request_headers", "pushed_authorization_request_endpoint_request_headers");
			addClientAuthenticationToPAREndpointRequest();

			profileBehavior.addParEndpointProfileHeaders(this);

			performParAuthorizationRequestFlow();
			env.unmapKey("request_headers");
		} else {
			eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
			buildRedirect();
			performRedirect();
		}
	}

	protected void buildRedirect() {
		callAndStopOnFailure(BuildRequestObjectByValueRedirectToAuthorizationEndpoint.class);
	}

	private static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {

		private boolean isSecondClient;
		private boolean isOpenId;
		private boolean isJarm;
		private boolean usePkce;
		private Class <? extends ConditionSequence> profileAuthorizationEndpointSetupSteps;

		public CreateAuthorizationRequestSteps(boolean isSecondClient,
											boolean isOpenId,
											boolean isJarm,
											boolean usePkce,
											Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps) {
			this.isSecondClient = isSecondClient;
			this.isOpenId = isOpenId;
			this.isJarm = isJarm;
			// it would probably be preferable to use the 'skip' syntax instead of the 'usePkce' flag, but it's
			// currently not possible to use 'skip' to skip a conditionsequence within a condition sequence
			this.usePkce = usePkce;
			this.profileAuthorizationEndpointSetupSteps = profileAuthorizationEndpointSetupSteps;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			if (profileAuthorizationEndpointSetupSteps != null) {
				call(sequence(profileAuthorizationEndpointSetupSteps));
			}

			if (isSecondClient) {
				call(exec().putInteger("requested_state_length", 128));
			} else {
				call(exec().removeNativeValue("requested_state_length"));
			}

			callAndStopOnFailure(CreateRandomStateValue.class);
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			if (isOpenId) {
				callAndStopOnFailure(CreateRandomNonceValue.class);
				callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
			}

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
			if (isJarm) {
				callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);
			}

			if (usePkce) {
				call(new SetupPkceAndAddToAuthorizationRequest());
			}
		}

	}

	protected void createAuthorizationRequest() {
		call(makeCreateAuthorizationRequestSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = new CreateAuthorizationRequestSteps(isSecondClient(), isOpenId, jarm, usePkce, profileAuthorizationEndpointSetupSteps);
		profileBehavior.customizeAuthorizationRequest(seq, this);
		if (isRarRequest){
			seq.then(condition(RARSupport.AddRARToAuthorizationEndpointRequest.class));
		}
		return seq;
	}

	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return makeCreateAuthorizationRequestSteps(true);
	}

	public static class CreateAuthorizationRequestObjectSteps extends AbstractConditionSequence {

		protected boolean isSecondClient;
		protected boolean encrypt;

		public CreateAuthorizationRequestObjectSteps(boolean isSecondClient, boolean encrypt) {
			this.isSecondClient = isSecondClient;
			this.encrypt = encrypt;
		}

		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

			if (isSecondClient) {
				callAndStopOnFailure(AddIatToRequestObject.class);
			}
			callAndStopOnFailure(AddNbfToRequestObject.class, "FAPI2-MS-ID1-5.3.1-3"); // mandatory in FAPI2-Message-Signing-Final
			callAndStopOnFailure(AddExpToRequestObject.class, "FAPI2-MS-ID1-5.3.1-4");

			callAndStopOnFailure(AddAudToRequestObject.class, "FAPI2-SP-FINAL-5.3.2.1-6");

			// iss is a 'should' in OIDC & jwsreq,
			callAndStopOnFailure(AddIssToRequestObject.class, "OIDCC-6.1");

			// jwsreq-26 is very explicit that client_id should be both inside and outside the request object
			callAndStopOnFailure(AddClientIdToRequestObject.class, "JAR-5", "FAPI2-MS-ID1-5.3.2-1");

			if (isSecondClient) {
				callAndStopOnFailure(SignRequestObjectIncludeMediaType.class, "JAR-4");
			}
			else {
				callAndStopOnFailure(SignRequestObject.class);
			}

			if (encrypt) {
				callAndStopOnFailure(FAPIBrazilEncryptRequestObject.class, "BrazilOB-5.2.2-1", "BrazilOB-6.1.2");
			}
		}
	}

	protected void createAuthorizationRequestObject() {
		call(makeCreateAuthorizationRequestObjectSteps());
	}

	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		boolean encrypt = profileBehavior.encryptRequestObject(isPar);
		return new CreateAuthorizationRequestObjectSteps(isSecondClient(), encrypt);
	}

	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlFragmentForCodeFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		if (jarm) {
			callAndContinueOnFailure(ValidateSuccessfulJARMResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		} else {
			callAndContinueOnFailure(ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING);
		}

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5");

		callAndContinueOnFailure(RequireIssInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2", "FAPI2-SP-FINAL-5.3.2.2-7");

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}

	protected void createClientCredentialsGrantRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		addClientAuthenticationToTokenEndpointRequest();
	}

	protected void performPostAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Call token endpoint");

		// call the token endpoint and complete the flow
		if (clientCredentailsGrant) {
			createClientCredentialsGrantRequest();

			callSenderConstrainedTokenEndpoint();
			processTokenEndpointResponse();
		}
		else {
			createAuthorizationCodeRequest();
			exchangeAuthorizationCode();
		}
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		addClientAuthenticationToTokenEndpointRequest();

		addPkceCodeVerifier();

		profileBehavior.addTokenEndpointProfileHeaders(this);
	}

	protected void addPkceCodeVerifier() {
		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5", "FAPI2-SP-FINAL-5.3.3.2-3");
	}

	protected void addClientAuthenticationToTokenEndpointRequest() {
		if (env.getObject("token_endpoint_request_headers") == null) {
			env.putObject("token_endpoint_request_headers", new JsonObject());
		}
		env.mapKey("request_headers", "token_endpoint_request_headers");
		call(sequence(addTokenEndpointClientAuthentication));
		env.unmapKey("request_headers");
	}

	protected void addClientAuthenticationToPAREndpointRequest() {
		call(sequence(addParEndpointClientAuthentication));
	}


	/**
	 * Call sender constrained token endpoint. For DPOP nonce errors, it will retry with new server nonce value.
	 * @param fullResponse whether the full response should be returned
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(boolean fullResponse, String... requirements) {
		final int MAX_RETRY = 2;

		if (isDpop()) {
			int i = 0;
			while(i < MAX_RETRY){
				createDpopForTokenEndpoint();
				callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class, requirements);
				if(Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(fullResponse ? CallTokenEndpointAndReturnFullResponse.class : CallTokenEndpoint.class, requirements);
		}
	}

	/**
	 * Call sender constrained token endpoint returning full response
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse)
	 */
	protected void callSenderConstrainedTokenEndpointAndStopOnFailure(String... requirements) {
		callSenderConstrainedTokenEndpointAndStopOnFailure(true, requirements);
	}

	/**
	 * Default Call to sender constrained token endpoint with non-full response
	 */
	protected void callSenderConstrainedTokenEndpoint() {
		callSenderConstrainedTokenEndpointAndStopOnFailure(false);
	}

	protected void exchangeAuthorizationCode() {
		callSenderConstrainedTokenEndpointAndStopOnFailure(true);

		eventLog.startBlock(currentClientString() + "Verify token endpoint response");
		processTokenEndpointResponse();
		eventLog.endBlock();
	}

	protected void processTokenEndpointResponse() {
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "RFC6749-4.1.4");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.WARNING, "RFC6749-5.1");
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
		profileBehavior.validateTokenExpiresIn(this);
		// scope is not *required* to be returned as the request was passed in signed request object - FAPI-R-5.2.2-15
		// https://gitlab.com/openid/conformance-suite/issues/617

		if (clientCredentailsGrant) {
			// FIXME: Treating as a warning until there is a resolution in:
			// https://bitbucket.org/openid/fapi/issues/756/certification-team-query-refresh-tokens-in
			callAndContinueOnFailure(EnsureNoRefreshTokenInTokenResponse.class, ConditionResult.WARNING, "RFC6749-4.4.3");
		}
		else {
			callAndContinueOnFailure(CheckForRefreshTokenValue.class, ConditionResult.INFO);
		}

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		skipIfElementMissing("token_endpoint_response", "refresh_token", Condition.ConditionResult.INFO,
			EnsureMinimumRefreshTokenEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10");

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4.1-4");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.4.1-4");

		if (isOpenId) {
			skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
				ValidateIdTokenFromTokenResponseEncryption.class, Condition.ConditionResult.WARNING, "OIDCC-10.2");
			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI2-SP-FINAL-5.3.2.3", "OIDCC-3.3.2.5");

			call(new PerformStandardIdTokenChecks());

			callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

			performProfileIdTokenValidation();

			profileBehavior.validateIdTokenSigningAlg(this);
			profileBehavior.validateIdTokenEncryption(this);

			// code flow - all hashes are optional.
			callAndContinueOnFailure(ExtractCHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");
			callAndContinueOnFailure(ExtractSHash.class, ConditionResult.INFO, "FAPI1-ADV-5.2.2.1-5");
			callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

			/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
			 * determined by the Extract*Hash condition
			 */
			skipIfMissing(new String[]{"c_hash"}, null, Condition.ConditionResult.INFO,
				ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
			skipIfMissing(new String[]{"s_hash"}, null, Condition.ConditionResult.INFO,
				ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
			skipIfMissing(new String[]{"at_hash"}, null, Condition.ConditionResult.INFO,
				ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}
		else {
			callAndStopOnFailure(ExpectNoIdTokenInTokenResponse.class);
		}

		if (isRarRequest){
			callAndStopOnFailure(RARSupport.CheckForAuthorizationDetailsInTokenResponse.class, "RAR-7");
		}

		profileBehavior.validateTokenResponseProfileHeaders(this);
	}

	protected void createDpopForTokenEndpoint() {
		if(null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForTokenEndpointSteps) {
			call(sequence(createDpopForTokenEndpointSteps));
		}
	}

	protected void createDpopForParEndpoint() {

		if(null == env.getElementFromObject("client", "dpop_private_jwk")) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		if (null != createDpopForParEndpointSteps) {
			call(sequence(createDpopForParEndpointSteps));
		}
	}

	@Override
	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		if (jarm) {
			processCallbackForJARM();
		} else {
			// FAPI2 always requires the auth code flow, use the query as the response
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		}
		callAndContinueOnFailure(RejectErrorInUrlFragment.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		callAndContinueOnFailure(RejectAuthCodeInUrlFragment.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		onAuthorizationCallbackResponse();

		eventLog.endBlock();
	}

	/**
	 * For error responses, we allow a JARM response, or an error page or a plain (non-jarm) error response
	 * per https://gitlab.com/openid/conformance-suite/-/issues/860
	 */
	protected void processCallbackForJARM() {
		String errorParameter = env.getString("callback_query_params", "error");
		String responseParameter = env.getString("callback_query_params", "response");
		if(allowPlainErrorResponseForJarm && responseParameter==null && errorParameter!=null) {
			//plain error response, no jarm
			callAndStopOnFailure(AddPlainErrorResponseAsAuthorizationEndpointResponseForJARM.class);
		} else {
			skipIfMissing(new String[]{"client_jwks"}, null, Condition.ConditionResult.INFO,
				ValidateJARMFromURLQueryEncryption.class, Condition.ConditionResult.WARNING, "JARM-2.2");
			callAndStopOnFailure(ExtractJARMFromURLQuery.class, "FAPI2-MS-ID1-5.4.2-2", "JARM-2.3.4", "JARM-2.3.1");

			callAndContinueOnFailure(RejectNonJarmResponsesInUrlQuery.class, ConditionResult.FAILURE, "JARM-2.1");

			callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromJARMResponse.class);

			callAndContinueOnFailure(ValidateJARMResponse.class, ConditionResult.FAILURE, "JARM-2.4-2", "JARM-2.4-3", "JARM-2.4-4");

			callAndContinueOnFailure(FAPI2ValidateJarmSigningAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jws_header", ConditionResult.INFO,
				ValidateJARMSigningAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jwe_header", ConditionResult.INFO,
				ValidateJARMEncryptionAlg.class, ConditionResult.FAILURE);

			skipIfElementMissing("jarm_response", "jwe_header", ConditionResult.INFO,
				ValidateJARMEncryptionEnc.class, ConditionResult.FAILURE);

			callAndContinueOnFailure(ValidateJARMExpRecommendations.class, ConditionResult.WARNING, "JARM-2.1");

			callAndContinueOnFailure(ValidateJARMSignatureUsingKid.class, ConditionResult.FAILURE, "JARM-2.4-5");
		}
	}


	protected void performProfileIdTokenValidation() {
		if (profileIdTokenValidationSteps != null) {
			call(sequence(profileIdTokenValidationSteps));
		}
	}

	public static class UpdateResourceRequestSteps extends AbstractConditionSequence {

		protected Supplier <? extends ConditionSequence> createDpopForResourceEndpointSteps;
		protected boolean brazilPayments;

		public UpdateResourceRequestSteps(Supplier <? extends ConditionSequence> createDpopForResourceEndpointSteps, boolean brazilPayments) {
			this.createDpopForResourceEndpointSteps = createDpopForResourceEndpointSteps;
			this.brazilPayments = brazilPayments;
		}

		@Override
		public void evaluate() {
			if (createDpopForResourceEndpointSteps != null) {
				call(sequence(createDpopForResourceEndpointSteps));
			}
			if (brazilPayments) {
				// we use the idempotency header to allow us to make a request more than once; however it is required
				// that a new jwt is sent in each retry, so update jti/iat & resign
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
				call(exec().unmapKey("request_object_claims"));
				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		}
	}

	protected ConditionSequence makeUpdateResourceRequestSteps() {
		return new UpdateResourceRequestSteps(createDpopForResourceEndpointSteps, brazilPayments);
	}

	// Make any necessary updates to a resource request before we send it again
	protected void updateResourceRequest() {
		call(makeUpdateResourceRequestSteps());
	}

	protected void updateResourceRequestAndCallProtectedResourceUsingDpop(String ... requirements) {
		if (isDpop()) {
			final int MAX_RETRY = 2;
			int i = 0;
			while(i < MAX_RETRY) {
				updateResourceRequest();
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, requirements);
				if(Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break; // no nonce error so
				}
				// continue call with nonce
				++i;
			}
		}
	}

	protected void requestProtectedResourceUsingDpop() {
		if (isDpop() && (createDpopForResourceEndpointSteps != null) ) {
			final int MAX_RETRY = 2;
			int i = 0;
			while(i < MAX_RETRY) {
				call(sequence(createDpopForResourceEndpointSteps));
				callAndStopOnFailure(CallProtectedResourceAllowingDpopNonceError.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
				if(Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
					break; // no nonce error so
				}
				// continue call with nonce
				++i;
			}
		}
	}

	protected void requestProtectedResource() {
		profileBehavior.requestProtectedResource(this);
	}

	/**
	 * Default resource endpoint request flow: FAPI2 standard resource call.
	 * Called by profile behaviors that don't override the resource endpoint flow.
	 */
	public void defaultRequestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		profileBehavior.addResourceEndpointProfileHeaders(this, isSecondClient());

		profileBehavior.setupResourceRequestBody(this);

		boolean mtlsRequired = getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS ||
			profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}

		if (isDpop() ) {
			requestProtectedResourceUsingDpop();
		} else  {
			callAndStopOnFailure(CallProtectedResource.class, "FAPI2-SP-FINAL-5.3.4-2");
		}
		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "RFC7231-7.1.1.2");

		profileBehavior.validateResourceResponseProfileHeaders(this, isSecondClient());

		profileBehavior.validateResourceResponseBody(this);

		eventLog.endBlock();
	}

	private void validateBrazilPaymentInitiationSignedResponseInternal() {
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseTyp.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		// signature needs to be validated against the organisation jwks (already fetched during pre-auth steps)

		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));
		callAndStopOnFailure(FetchServerKeys.class);
		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		callAndContinueOnFailure(ValidateResourceResponseSignature.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));
	}

	/**
	 * Default resource endpoint headers: add optional FAPI headers for first client only,
	 * plus interaction ID for first client.
	 * Called by profile behaviors that don't fully override resource endpoint headers.
	 */
	public void defaultAddResourceEndpointProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			// these are optional; only add them for the first client
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "CDR-http-headers");
			callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "CDR-http-headers");
			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "CID-SP-4.2-12", "CDR-http-headers");
		}
	}

	/**
	 * Default resource response header validation: check interaction ID for first client only.
	 * Called by profile behaviors that don't override resource response header validation.
	 */
	public void defaultValidateResourceResponseProfileHeaders(boolean isSecondClient) {
		if (!isSecondClient) {
			skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO,
				CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
			skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id", ConditionResult.INFO,
				EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "CID-SP-4.2-12", "FAPI2-IMP-2.1.1");
		}
	}

	/**
	 * Setup the Brazil payments resource request body (signed JWT).
	 * Called by OpenBankingBrazilProfileBehavior.
	 */
	public void setupBrazilPaymentsResourceRequestBody() {
		if (!brazilPayments) {
			return;
		}
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

	/**
	 * Validate Brazil payment initiation signed response.
	 * Public so it can be called by OpenBankingBrazilProfileBehavior.
	 */
	public void validateBrazilPaymentInitiationSignedResponse() {
		if (!brazilPayments) {
			return;
		}
		validateBrazilPaymentInitiationSignedResponseInternal();
	}

	public boolean isSecondClient() {
		return whichClient == 2;
	}

	// --- Package-visible accessors for profile behavior classes ---

	net.openid.conformance.testmodule.Environment getEnv() {
		return env;
	}

	Class<? extends ConditionSequence> getAddTokenEndpointClientAuthentication() {
		return addTokenEndpointClientAuthentication;
	}

	// Delegate methods exposing protected AbstractTestModule methods to profile behaviors
	void doCallAndStopOnFailure(Class<? extends net.openid.conformance.condition.Condition> conditionClass, String... requirements) {
		callAndStopOnFailure(conditionClass, requirements);
	}

	void doCallAndContinueOnFailure(Class<? extends net.openid.conformance.condition.Condition> conditionClass, ConditionResult onFail, String... requirements) {
		callAndContinueOnFailure(conditionClass, onFail, requirements);
	}

	void doSkipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends net.openid.conformance.condition.Condition> conditionClass, ConditionResult onFail, String... requirements) {
		skipIfMissing(required, strings, onSkip, conditionClass, onFail, requirements);
	}

	void doSkipIfElementMissing(String objId, String path, ConditionResult onSkip,
		Class<? extends net.openid.conformance.condition.Condition> conditionClass, ConditionResult onFail, String... requirements) {
		skipIfElementMissing(objId, path, onSkip, conditionClass, onFail, requirements);
	}

	net.openid.conformance.testmodule.ConditionCallBuilder doCondition(Class<? extends net.openid.conformance.condition.Condition> conditionClass) {
		return condition(conditionClass);
	}

	boolean doIsDpop() {
		return isDpop();
	}

	boolean doIsMtlsRequired() {
		return getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS ||
			profileRequiresMtlsEverywhere;
	}

	void doRequestProtectedResourceUsingDpop() {
		requestProtectedResourceUsingDpop();
	}

	net.openid.conformance.testmodule.Command doExec() {
		return exec();
	}

	void doCall(net.openid.conformance.testmodule.TestExecutionUnit executionUnit) {
		call(executionUnit);
	}

	void doStartBlock(String blockName) {
		eventLog.startBlock(blockName);
	}

	void doEndBlock() {
		eventLog.endBlock();
	}

	void doLog(String msg) {
		eventLog.log(getName(), msg);
	}

	String doCurrentClientString() {
		return currentClientString();
	}

	void doCallSequence(Class<? extends ConditionSequence> sequenceClass) {
		call(sequence(sequenceClass));
	}

	void doFireTestSkipped(String msg) {
		fireTestSkipped(msg);
	}

	/**
	 * Return which client is in use, for use in block identifiers
	 */
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		}
		return "";
	}

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		addParEndpointClientAuthentication = AddMTLSClientAuthenticationToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest.class;

		if (getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS) {
			supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		}

		addParEndpointClientAuthentication = CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_attestation")
	public void setupClientAttestation() {
		addTokenEndpointClientAuthentication = AddClientAttestationClientAuthToEndpointRequest.class;
		if (getVariant(FAPI2SenderConstrainMethod.class) == FAPI2SenderConstrainMethod.MTLS) {
			supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
		}
		addParEndpointClientAuthentication = AddClientAttestationClientAuthToEndpointRequest.class;
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		profileBehavior = new FAPI2ProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "fapi_client_credentials_grant")
	public void setupFapiClientCredentialsGrant() {
		profileBehavior = new ClientCredentialsGrantProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		profileBehavior = new OpenBankingUkProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "consumerdataright_au")
	public void setupConsumerDataRightAu() {
		profileBehavior = new ConsumerDataRightAuProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		profileBehavior = new OpenBankingBrazilProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "connectid_au")
	public void setupConnectId() {
		profileBehavior = new ConnectIdAuProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "cbuae")
	public void setupCbuaeFapi() {
		profileBehavior = new CbuaeProfileBehavior();
		configureFromProfileBehavior();
	}

	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "vci")
	public void setupVci() {
		profileBehavior = new VCIProfileBehavior();
		configureFromProfileBehavior();
	}

	private void configureFromProfileBehavior() {
		resourceConfiguration = profileBehavior.resourceConfiguration();
		preAuthorizationSteps = profileBehavior.preAuthorizationSteps(this);
		profileAuthorizationEndpointSetupSteps = profileBehavior.profileAuthorizationEndpointSetupSteps();
		profileIdTokenValidationSteps = profileBehavior.profileIdTokenValidationSteps();
	}

	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		createDpopForParEndpointSteps = () -> CreateDpopProofSteps.createParEndpointDpopSteps();
		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps();
		createDpopForResourceEndpointSteps = () -> CreateDpopProofSteps.createResourceEndpointDpopSteps();
	}

	protected boolean scopeContains(String requiredScope) {
		String scope = env.getString("config", "client.scope");
		if (Strings.isNullOrEmpty(scope)) {
			return false;
		}
		List<String> scopes = Arrays.asList(scope.split(" "));
		return scopes.contains(requiredScope);
	}

	protected void updatePaymentConsent() {
		callAndStopOnFailure(FAPIBrazilSetPaymentDateToToday.class);
	}

	protected ConditionSequence createOBBPreauthSteps() {
		if (brazilPayments) {
			eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
			updatePaymentConsent();
		}
		OpenBankingBrazilPreAuthorizationSteps steps = new OpenBankingBrazilPreAuthorizationSteps(
			isSecondClient(),
			isDpop(),
			addTokenEndpointClientAuthentication,
			brazilPayments,
			false, // open insurance not yet supported in fapi2
			false,
			false);
		return steps;
	}

	protected void performPARRedirectWithRequestUri() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint.class, "PAR-4");
		performRedirect();
	}


	/**
	 * Call Par endpoint with retry for DPoP nonce error
	 * @param requirements requirements are the same as original call to callAndStopOnFailure(CallParEndpoint)
	 */
	protected void callParEndpointAndStopOnFailure(String... requirements) {
		if (isDpop() && useDpopAuthCodeBinding) {
			final int MAX_RETRY = 2;
			int i = 0;
			while(i < MAX_RETRY){
				createDpopForParEndpoint();
				callAndStopOnFailure(CallPAREndpointAllowingDpopNonceError.class, requirements);
				if(Strings.isNullOrEmpty(env.getString("par_endpoint_dpop_nonce_error"))) {
					break;
				}
				++i;
			}
		} else {
			callAndStopOnFailure(CallPAREndpoint.class, requirements);
		}
	}


	protected void performParAuthorizationRequestFlow() {

		// we only need to (and only should) supply an MTLS authentication when using MTLS client auth;
		// there's no need to pass mtls auth when using private_key_jwt
		boolean mtlsRequired = getVariant(ClientAuthType.class) == ClientAuthType.MTLS ||
			profileRequiresMtlsEverywhere;

		JsonObject mtls = null;
		if (!mtlsRequired) {
			mtls = env.getObject("mutual_tls_authentication");
			env.removeObject("mutual_tls_authentication");
		}

		callParEndpointAndStopOnFailure("PAR-2.1");

		if (!mtlsRequired && mtls != null) {
			env.putObject("mutual_tls_authentication", mtls);
		}

		processParResponse();
	}

	protected void processParResponse() {
		callAndStopOnFailure(CheckPAREndpointResponse201WithNoError.class, "PAR-2.2", "PAR-2.3", "PAR-2.4");

		callAndStopOnFailure(CheckForRequestUriValue.class, "PAR-2.2");

		callAndContinueOnFailure(CheckForPARResponseExpiresIn.class, ConditionResult.FAILURE, "PAR-2.2");

		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		env.unmapKey("endpoint_response");

		callAndStopOnFailure(ExtractRequestUriFromPARResponse.class);

		callAndContinueOnFailure(EnsureMinimumRequestUriEntropy.class, ConditionResult.FAILURE, "PAR-2.2", "PAR-7.1", "JAR-10.2");

		profileBehavior.validateParResponseProfileHeaders(this);

		performPARRedirectWithRequestUri();
	}
}
