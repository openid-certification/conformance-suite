package net.openid.conformance.vci10wallet;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCodeChallengeMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddDpopSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddIdTokenSigningAlgsToServerConfiguration;
import net.openid.conformance.condition.as.AddIssSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddIssToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddResponseTypeCodeToServerConfiguration;
import net.openid.conformance.condition.as.AddSHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddSupportedAuthorizationTypesToServerConfiguration;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CalculateSHash;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInRequestObject;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CopyAccessTokenToDpopClientCredentialsField;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateAuthorizationServerDpopNonce;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreatePAREndpointDpopErrorResponse;
import net.openid.conformance.condition.as.CreatePAREndpointInvalidClientErrorResponse;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.CreateMdocCredentialForVCI;
import net.openid.conformance.condition.as.CreateSdJwtCredential;
import net.openid.conformance.condition.as.CreateTokenEndpointDpopErrorResponse;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsStateParameter;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureMatchingRedirectUriInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsurePAREndpointRequestDoesNotContainRequestUriParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ExtractParAuthorizationCodeDpopBindingKey;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPI2ValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.FAPIBrazilExtractConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentInitiationRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentsConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentConsentResponse;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentInitiationResponse;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectExp;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectMediaType;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateCredentialNonce;
import net.openid.conformance.condition.as.GenerateCredentialNonceResponse;
import net.openid.conformance.condition.as.GenerateDpopAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateServerConfigurationMTLS;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SetRsaAltServerJwks;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToAttestJwtClientAuthOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateClientAssertionAudClaimIsIssuerAsString;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientAssertionClaimsForPAREndpoint;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.as.ValidateRedirectUri;
import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.jarm.GenerateJARMResponseClaims;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR;
import net.openid.conformance.condition.as.par.ExtractRequestObjectFromPAREndpointRequest;
import net.openid.conformance.condition.client.AugmentRealJwksWithDecoys;
import net.openid.conformance.condition.client.BuildVCIDCAPIRequest;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.condition.common.RARSupport.EnsureEffectiveAuthorizationEndpointRequestContainsValidRAR;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse;
import net.openid.conformance.condition.rs.CreateOpenBankingAccountRequestResponse;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorResponse;
import net.openid.conformance.condition.rs.CreateResourceServerDpopNonce;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsGet;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractDpopAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractDpopProofFromHeader;
import net.openid.conformance.condition.rs.ExtractFapiDateHeader;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader;
import net.openid.conformance.condition.rs.ExtractXIdempotencyKeyHeader;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainPayments;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureClientCredentialsScopeContainedConsents;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureClientCredentialsScopeContainedPayments;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureConsentRequestJtiIsUUIDv4;
import net.openid.conformance.condition.rs.FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId;
import net.openid.conformance.condition.rs.FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4;
import net.openid.conformance.condition.rs.FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate;
import net.openid.conformance.condition.rs.FAPIBrazilExtractCertificateSubjectFromServerJwks;
import net.openid.conformance.condition.rs.FAPIBrazilFetchClientOrganizationJwksFromDirectory;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateGetConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateGetPaymentConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewPaymentInitiationResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewPaymentsConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.condition.rs.FAPIBrazilValidateConsentRequestIat;
import net.openid.conformance.condition.rs.FAPIBrazilValidateJwtSignatureUsingOrganizationJwks;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentConsentRequestAud;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentInitiationRequestAud;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentInitiationRequestIat;
import net.openid.conformance.condition.rs.GenerateAccountRequestId;
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.RequireDpopAccessToken;
import net.openid.conformance.condition.rs.RequireDpopClientCredentialAccessToken;
import net.openid.conformance.condition.rs.RequireMtlsAccessToken;
import net.openid.conformance.condition.rs.RequireMtlsClientCredentialsAccessToken;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddPARToServerConfiguration;
import net.openid.conformance.sequence.as.PerformDpopProofParRequestChecks;
import net.openid.conformance.sequence.as.PerformDpopProofResourceRequestChecks;
import net.openid.conformance.sequence.as.PerformDpopProofTokenRequestChecks;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.util.TemplateProcessor;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VCICredentialEncryption;
import net.openid.conformance.variant.VCICredentialIssuanceMode;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import net.openid.conformance.variant.VCIGrantType;
import net.openid.conformance.variant.VCIProfile;
import net.openid.conformance.variant.VCIWalletAuthorizationCodeFlowVariant;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10wallet.condition.VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCICheckIssuerMetadataRequestUrl;
import net.openid.conformance.vci10wallet.condition.VCICheckOAuthAuthorizationServerMetadataRequestUrl;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialEndpointResponse;
import net.openid.conformance.vci10wallet.condition.VCICreateDeferredCredentialResponse;
import net.openid.conformance.vci10wallet.condition.VCIEncryptCredentialResponse;
import net.openid.conformance.vci10wallet.condition.VCIValidateDeferredCredentialRequest;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOffer;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferRedirectUrl;
import net.openid.conformance.vci10wallet.condition.VCICreateCredentialOfferUri;
import net.openid.conformance.vci10wallet.condition.VCIEnsureCredentialSigningCertificateIsNotSelfSigned;
import net.openid.conformance.vci10wallet.condition.VCIExtractCredentialRequestProof;
import net.openid.conformance.vci10wallet.condition.VCIGenerateIssuerState;
import net.openid.conformance.vci10wallet.condition.VCIGenerateSignedCredentialIssuerMetadata;
import net.openid.conformance.vci10wallet.condition.VCIInjectAuthorizationDetailsForPreAuthorizedCodeFlow;
import net.openid.conformance.vci10wallet.condition.VCIInjectCredentialConfigurationIdHint;
import net.openid.conformance.vci10wallet.condition.VCIInjectOpenIdCredentialAsSupportedAuthorizationRequestTypes;
import net.openid.conformance.vci10wallet.condition.VCILogGeneratedCredentialIssuerMetadata;
import net.openid.conformance.vci10wallet.condition.VCIPreparePreAuthorizationCode;
import net.openid.conformance.vci10wallet.condition.VCIResolveRequestedCredentialConfigurationFromRequest;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestAttestationProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestDiVpProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestJwtProof;
import net.openid.conformance.vci10wallet.condition.VCIValidateCredentialRequestStructure;
import net.openid.conformance.vci10wallet.condition.VCIValidatePreAuthorizationCode;
import net.openid.conformance.vci10wallet.condition.VCIValidateTxCode;
import net.openid.conformance.vci10wallet.condition.VCIVerifyIssuerStateInAuthorizationRequest;
import net.openid.conformance.vci10wallet.condition.clientattestation.AddClientAttestationPoPNonceRequiredToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.AddClientAttestationSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterKeyAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIValidateClientAuthenticationWithClientAttestationJWT;
import net.openid.conformance.vci10wallet.condition.statuslist.VCIGenerateJwtStatusListToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@VariantParameters({
	VCIClientAuthType.class,
	FAPI2AuthRequestMethod.class,
	FAPI2SenderConstrainMethod.class,
	AuthorizationRequestType.class,
	VCIProfile.class,
	VCIGrantType.class,
	VCIWalletAuthorizationCodeFlowVariant.class,
	VCICredentialOfferParameterVariant.class,
	VCI1FinalCredentialFormat.class,
	VCICredentialIssuanceMode.class,
	VCICredentialEncryption.class,
})
@VariantHidesConfigurationFields(parameter = VCIWalletAuthorizationCodeFlowVariant.class, value = "wallet_initiated", configurationFields = {
	"vci.credential_offer_endpoint"
})
@VariantHidesConfigurationFields(parameter = VCIWalletAuthorizationCodeFlowVariant.class, value = "issuer_initiated_dc_api", configurationFields = {
	"vci.credential_offer_endpoint"
})
@VariantHidesConfigurationFields(parameter = VCIClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"vci.client_attestation_issuer"
})
@VariantHidesConfigurationFields(parameter = VCIClientAuthType.class, value = "mtls", configurationFields = {
	"vci.client_attestation_issuer"
})
@VariantConfigurationFields(parameter = VCIClientAuthType.class, value = "client_attestation", configurationFields = {
	"vci.client_attestation_issuer", "vci.client_attestation_trust_anchor"
})
@VariantConfigurationFields(parameter = VCIClientAuthType.class, value = "mtls", configurationFields = {
	"client.certificate"
})
@VariantConfigurationFields(parameter = VCICredentialEncryption.class, value = "encrypted", configurationFields = {
	"vci.credential_encryption_jwks"
})
public abstract class AbstractVCIWalletTest extends AbstractTestModule {

	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";

	public static final String CREDENTIAL_PATH = "credential";

	public static final String CREDENTIAL_OFFER_PATH = "credential_offer";

	public static final String NONCE_PATH = "nonce";

	public static final String DEFERRED_CREDENTIAL_PATH = "deferred_credential";

	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	private Class<? extends ConditionSequence> configureResponseModeSteps;
	private Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	private Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;
	private Class<? extends Condition> generateSenderConstrainedAccessToken;
	private Class<? extends ConditionSequence> validateSenderConstrainedTokenSteps;  // for bearer tokens
	private Class<? extends ConditionSequence> validateSenderConstrainedClientCredentialAccessTokenSteps;  // client credential access tokens
	private SenderContrainTokenRequestHelper senderConstrainTokenRequestHelper;

	protected VCIProfile vciProfile;

	protected VCIClientAuthType clientAuthType;

	protected FAPI2SenderConstrainMethod fapi2SenderConstrainMethod;

	protected FAPI2AuthRequestMethod fapi2AuthRequestMethod;

	protected AuthorizationRequestType authorizationRequestType;

	protected boolean startingShutdown = false;

	protected Boolean profileRequiresMtlsEverywhere;

	protected long waitTimeoutSeconds = 5;

	protected VCIGrantType vciGrantType;

	protected VCIWalletAuthorizationCodeFlowVariant vciAuthorizationCodeFlowVariant;

	protected VCICredentialOfferParameterVariant vciCredentialOfferParameterVariantType;

	protected VCI1FinalCredentialFormat vciCredentialFormat;

	protected VCICredentialIssuanceMode vciCredentialIssuanceMode;

	protected VCICredentialEncryption vciCredentialEncryption;

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken() {
	}

	protected void addCustomValuesToAuthorizationResponse() {
	}

	protected void endTestIfRequiredParametersAreMissing() {
	}

	protected Boolean isDpopConstrain() {
		return fapi2SenderConstrainMethod == FAPI2SenderConstrainMethod.DPOP;
	}

	protected Boolean isMTLSConstrain() {
		return fapi2SenderConstrainMethod == FAPI2SenderConstrainMethod.MTLS;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		if (config.has("waitTimeoutSeconds")) {
			waitTimeoutSeconds = OIDFJSON.getLong(config.get("waitTimeoutSeconds"));
		}

		setupPlainFapi();
		setupResponseModePlain();
		clientAuthType = getVariant(VCIClientAuthType.class);
		fapi2AuthRequestMethod = getVariant(FAPI2AuthRequestMethod.class);
		fapi2SenderConstrainMethod = getVariant(FAPI2SenderConstrainMethod.class);
		authorizationRequestType = getVariant(AuthorizationRequestType.class);

		vciProfile = getVariant(VCIProfile.class);
		vciCredentialFormat = getVariant(VCI1FinalCredentialFormat.class);
		vciCredentialIssuanceMode = getVariant(VCICredentialIssuanceMode.class);
		vciCredentialEncryption = getVariant(VCICredentialEncryption.class);

		profileRequiresMtlsEverywhere = false;

		// We create a configuration that contains mtls_endpoint_aliases in all cases - it's mandatory for clients to
		// support it as per https://datatracker.ietf.org/doc/html/rfc8705#section-5
		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		//this must come before configureResponseModeSteps due to JARM signing_algorithm dependency
		configureServerJWKS();

		call(condition(AddResponseTypeCodeToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2-1"));
		call(condition(AddIssSupportedToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2-7"));
		call(condition(AddCodeChallengeMethodToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2"));

		callAndStopOnFailure(ExtractServerSigningAlg.class);

		callAndStopOnFailure(AddIdTokenSigningAlgsToServerConfiguration.class);

		if (fapi2AuthRequestMethod == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION) {
			callAndStopOnFailure(FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration.class);
		}

		checkCredentialSigningKey(env);

		vciGrantType = getVariant(VCIGrantType.class);
		if (vciGrantType == VCIGrantType.AUTHORIZATION_CODE) {
			callAndStopOnFailure(VCIGenerateIssuerState.class, "OID4VCI-1FINAL-5.1.3-2.1");
		}

		vciAuthorizationCodeFlowVariant = getVariant(VCIWalletAuthorizationCodeFlowVariant.class);

		vciCredentialOfferParameterVariantType = getVariant(VCICredentialOfferParameterVariant.class);

		if (isMTLSConstrain()) {
			callAndStopOnFailure(AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration.class, "FAPI2-4.3.1-9");
		} else if (isDpopConstrain()) {
			callAndStopOnFailure(AddDpopSigningAlgValuesSupportedToServerConfiguration.class, "DPOP-5.1");
		}

		if (addTokenEndpointAuthMethodSupported != null) {
			callAndStopOnFailure(addTokenEndpointAuthMethodSupported);
		}
		call(sequence(AddPARToServerConfiguration.class));

		if (configureResponseModeSteps != null) {
			call(sequence(configureResponseModeSteps));
		}

		callAndStopOnFailure(FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);

		if (clientAuthType == VCIClientAuthType.CLIENT_ATTESTATION) {
			if (env.getString("config", "vci.client_attestation_issuer") == null) {
				throw new TestFailureException(getId(), "vci.client_attestation_issuer must be configured if client_attestation is used as client authentication method.");
			}
			callAndStopOnFailure(AddClientAttestationPoPNonceRequiredToServerConfiguration.class, ConditionResult.FAILURE, "OAuth2-ATCA07-8-2");
			callAndStopOnFailure(AddClientAttestationSigningAlgValuesSupportedToServerConfiguration.class, "OAuth2-ATCA07-10.1");

			if (env.getString("config", "vci.client_attestation_trust_anchor") == null) {
				throw new TestFailureException(getId(), "vci.client_attestation_trust_anchor must be configured if client_attestation is used as client authentication method.");
			}
			callAndStopOnFailure(VCIRegisterClientAttestationTrustAnchor.class, ConditionResult.FAILURE);
		}

		callAndStopOnFailure(VCIRegisterKeyAttestationTrustAnchor.class);

		configureCredentialIssuerMetadata();
		configureOauthAuthorizationServerMetadata();

		exposeEnvString("credential_issuer");
		exposeEnvString("credential_issuer_metadata_url");
		exposeEnvString("oauth_authorization_server_metadata_url");

		callAndStopOnFailure(new VCIInjectCredentialConfigurationIdHint(getDefaultCredentialConfigurationId()), ConditionResult.FAILURE);
		exposeEnvString("credential_configuration_id_hint");

		if (authorizationRequestType == AuthorizationRequestType.RAR) {
			callAndStopOnFailure(VCIInjectOpenIdCredentialAsSupportedAuthorizationRequestTypes.class);
			callAndStopOnFailure(AddSupportedAuthorizationTypesToServerConfiguration.class);
		}

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");

		callAndStopOnFailure(LoadUserInfo.class);

		configureClients();

		configureOauthTokenStatusLists();

		onConfigurationCompleted();
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	protected String getDefaultCredentialConfigurationId() {
		if (vciCredentialFormat == VCI1FinalCredentialFormat.MDOC) {
			return "eu.europa.ec.eudi.pid.mdoc.1";
		}
		return "eu.europa.ec.eudi.pid.1";
	}

	protected void configureOauthTokenStatusLists() {
		JsonObject statusLists = new JsonObject();

		JsonObject statusList = new JsonObject();
		statusLists.add("status_list_1", statusList);

		env.putObject("vci", "status_lists", statusLists);
	}

	protected void configureOauthAuthorizationServerMetadata() {
		String oauthAuthorizationServerMetadataUrl = generateWellKnownUrlForPath(env.getString("credential_issuer"), "oauth-authorization-server");
		env.putString("oauth_authorization_server_metadata_url", oauthAuthorizationServerMetadataUrl);

		addTokenStatusListAggregationEndpointToOauthServerMetadata();
	}

	protected void addTokenStatusListAggregationEndpointToOauthServerMetadata() {
		String issuer = env.getString("server", "issuer");
		String listAggregationEndpoint = issuer + "statuslists";
		env.putString("server", "status_list_aggregation_endpoint", listAggregationEndpoint);
	}

	protected String generateWellKnownUrlForPath(String issuer, String wellKnownTypePath) {
		URI serverIssuerUri = URI.create(issuer);
		String serverIssuerPath = serverIssuerUri.getPath();
		String wellKnownBaseUrl = serverIssuerUri.getScheme() + "://" + serverIssuerUri.getAuthority() + "/.well-known";
		return wellKnownBaseUrl + "/" + wellKnownTypePath + serverIssuerPath;
	}

	protected void checkCredentialSigningKey(Environment env) {
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw new TestFailureException(getId(), "Credential Signing JWK missing from configuration.");
		}

		JWK credentialSigningJwk;
		try {
			credentialSigningJwk = JWK.parse(credentialSigningJwkEl.toString());
		} catch (ParseException e) {
			throw new TestFailureException(getId(), "Failed to create JWK from Credential Signing JWK: " + e.getMessage());
		}

		if (credentialSigningJwk.getX509CertChain() == null || credentialSigningJwk.getX509CertChain().isEmpty()) {
			throw new TestFailureException(getId(), "Credential Signing JWK must contain the certificate chain in the x5c claim.");
		}

		env.putString("vci", "credential_signing_jwk", credentialSigningJwkEl.toString());

		callAndStopOnFailure(VCIEnsureCredentialSigningCertificateIsNotSelfSigned.class, "HAIP-4.1");
	}

	protected void configureCredentialIssuerMetadata() {
		JsonObject credentialIssuerMetadata = getCredentialIssuerMetadata();
		env.putObject("credential_issuer_metadata", credentialIssuerMetadata);

		configureSupportedCredentialConfigurations();

		callAndStopOnFailure(VCILogGeneratedCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2",
			/* SD JWT VC: */ "OID4VCI-1FINALA-A.3.2",
			/* mdoc: */ "OID4VCI-1FINALA-A.2.2");
	}

	protected void generateSignedCredentialIssuerMetadata() {
		callAndStopOnFailure(VCIGenerateSignedCredentialIssuerMetadata.class, "OID4VCI-1FINAL-11.2.3", "HAIP-4.1");
	}

	protected JsonObject getCredentialIssuerMetadata() {

		String baseUrl = env.getString("base_url");
		String mtlsBaseUrl = env.getString("base_mtls_url");

		if (baseUrl.isEmpty()) {
			throw new TestFailureException(getId(), "Base URL is empty");
		}

		if (mtlsBaseUrl.isEmpty()) {
			throw new TestFailureException(getId(), "Base MTLS URL is empty");
		}

		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		if (!mtlsBaseUrl.endsWith("/")) {
			mtlsBaseUrl = mtlsBaseUrl + "/";
		}

		String credentialIssuer = baseUrl;
		String credentialEndpointUrl = (isMTLSConstrain() ? mtlsBaseUrl : baseUrl) + CREDENTIAL_PATH;
		String nonceEndpointUrl = (isMTLSConstrain() ? mtlsBaseUrl : baseUrl) + NONCE_PATH;
		String deferredCredentialEndpointUrl = (isMTLSConstrain() ? mtlsBaseUrl : baseUrl) + DEFERRED_CREDENTIAL_PATH;

		String metadata = TemplateProcessor.process("""
			{
				"credential_issuer": "$(credentialIssuer)",
				"credential_endpoint": "$(credentialEndpoint)",
				"nonce_endpoint": "$(nonceEndpoint)",
				"deferred_credential_endpoint": "$(deferredCredentialEndpoint)",
				"authorization_servers": [ "$(credentialIssuer)" ]
			}
			""", Map.of(
			"credentialIssuer", credentialIssuer,
			"credentialEndpoint", credentialEndpointUrl,
			"nonceEndpoint", nonceEndpointUrl,
			"deferredCredentialEndpoint", deferredCredentialEndpointUrl
		));

		String credentialIssuerMetadataUrl = generateWellKnownUrlForPath(credentialIssuer, "openid-credential-issuer");
		env.putString("credential_issuer_metadata_url", credentialIssuerMetadataUrl);

		env.putString("credential_issuer", credentialIssuer);
		env.putString("credential_issuer_nonce_endpoint_url", nonceEndpointUrl);
		env.putString("credential_issuer_credential_endpoint_url", credentialEndpointUrl);
		env.putString("credential_issuer_deferred_credential_endpoint_url", deferredCredentialEndpointUrl);

		JsonObject metadataJson = JsonParser.parseString(metadata).getAsJsonObject();

		// Add credential response encryption metadata if encryption is enabled
		if (vciCredentialEncryption == VCICredentialEncryption.ENCRYPTED) {
			JsonArray algValues = new JsonArray();
			algValues.add("ECDH-ES");
			algValues.add("ECDH-ES+A256KW");
			algValues.add("ECDH-ES+A128KW");
			metadataJson.add("credential_response_encryption_alg_values_supported", algValues);

			JsonArray encValues = new JsonArray();
			encValues.add("A256GCM");
			encValues.add("A128GCM");
			encValues.add("A256CBC-HS512");
			encValues.add("A128CBC-HS256");
			metadataJson.add("credential_response_encryption_enc_values_supported", encValues);
		}

		return metadataJson;
	}

	protected void configureSupportedCredentialConfigurations() {

		JsonObject supportedCredentialConfigurations = getSupportedCredentialConfigurations();
		env.getObject("credential_issuer_metadata").add("credential_configurations_supported", supportedCredentialConfigurations);

		JsonObject scopeToCredentialMap = new JsonObject();

		for (var configurationId : supportedCredentialConfigurations.keySet()) {
			JsonObject credentialConfiguration = supportedCredentialConfigurations.getAsJsonObject(configurationId);
			if (credentialConfiguration.has("scope")) {
				String scope = OIDFJSON.getString(credentialConfiguration.get("scope"));
				scopeToCredentialMap.addProperty(scope, configurationId);
			}
		}

		env.putObject("credential_configuration_id_scope_map", scopeToCredentialMap);
	}

	protected JsonObject getSupportedCredentialConfigurations() {

		String json = """
			{
				"eu.europa.ec.eudi.pid.1": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.attestation": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.jwt.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: JWT Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.attestation.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"attestation": {
							 "proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.jwt_and_attestation.keyattest": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"cryptographic_binding_methods_supported": [ "jwk" ],
					"credential_signing_alg_values_supported": [ "ES256" ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						},
						"attestation": {
							 "proof_signing_alg_values_supported": [ "ES256" ],
							 "key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID: JWT and Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID description"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc)",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.attestation": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc)",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.jwt.keyattest": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							"key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc): JWT Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.mdoc.1.attestation.keyattest": {
					"format": "mso_mdoc",
					"doctype": "eu.europa.ec.eudi.pid.1",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ],
							"key_attestations_required": {}
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (mdoc): Attestation Proof with Key Attestation",
							"description": "OpenID Conformance Test Fake PID in mso_mdoc format"
						}
						]
					}
				},
				"org.iso.18013.5.1.mDL": {
					"format": "mso_mdoc",
					"doctype": "org.iso.18013.5.1.mDL",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"jwt": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake mDL (ISO 18013-5)",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					}
				},
				"org.iso.18013.5.1.mDL.attestation": {
					"format": "mso_mdoc",
					"doctype": "org.iso.18013.5.1.mDL",
					"cryptographic_binding_methods_supported": [ "cose_key" ],
					"credential_signing_alg_values_supported": [ -7 ],
					"proof_types_supported": {
						"attestation": {
							"proof_signing_alg_values_supported": [ "ES256" ]
						}
					},
					"credential_metadata": {
						"display": [
						{
							"name": "Fake mDL (ISO 18013-5)",
							"description": "OpenID Conformance Test Fake Mobile Driver's License"
						}
						]
					}
				},
				"eu.europa.ec.eudi.pid.1.nobinding": {
					"format": "dc+sd-jwt",
					"vct": "urn:eudi:pid:1",
					"credential_signing_alg_values_supported": [ "ES256" ],
					"credential_metadata": {
						"display": [
						{
							"name": "Fake PID (No Holder Binding)",
							"description": "OpenID Conformance Test Fake PID without cryptographic holder binding"
						}
						]
					}
				}
			}
			""";

		JsonObject supportedCredentials = JsonParser.parseString(json).getAsJsonObject();

		return customizeSupportedCredentials(supportedCredentials);
	}

	protected JsonObject customizeSupportedCredentials(JsonObject supportedCredentials) {
		return supportedCredentials;
	}

	/**
	 * will be called at the end of configure
	 */
	protected void onConfigurationCompleted() {
		if (requireAuthorizationServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.INFO);
		}
		if (requireResourceServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
		}
	}

	protected boolean requireAuthorizationServerEndpointDpopNonce() {
		return isDpopConstrain();
	}

	protected boolean requireResourceServerEndpointDpopNonce() {
		return isDpopConstrain();
	}

	protected void configureClients() {
		eventLog.startBlock("Verify configuration of first client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks(false);
		validateClientConfiguration();

		eventLog.endBlock();
	}

	// This is currently unused as FAPI2 doesn't have the encrypted id token tests that
	// used the second client. We may want to delete it and all the associated references
	// to the second client if we find no use.
	protected void configureSecondClient() {
		eventLog.startBlock("Verify configuration of second client");
		// extract second client
		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);

		validateClientJwks(true);
		validateClientConfiguration();

		//switch back to the first client
		unmapClient();
		eventLog.endBlock();
	}

	protected void validateClientConfiguration() {
	}


	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
	}

	protected void validateClientJwks(boolean isSecondClient) {
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class, "FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");
	}

	protected void configureServerJWKS() {
		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(AugmentRealJwksWithDecoys.class, ConditionResult.WARNING, "FAPI2-SP-ID2-5.6.4-2.3.1");
		callAndStopOnFailure(SetRsaAltServerJwks.class);
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		switch (vciGrantType) {
			case AUTHORIZATION_CODE -> {
				switch (getVariant(VCIWalletAuthorizationCodeFlowVariant.class)) {
					case WALLET_INITIATED -> {
					}
					case ISSUER_INITIATED, ISSUER_INITIATED_DC_API -> prepareCredentialOffer();
				}
			}
			case PRE_AUTHORIZATION_CODE -> {
				switch (getVariant(VCIWalletAuthorizationCodeFlowVariant.class)) {
					case WALLET_INITIATED -> {
						throw new UnsupportedOperationException("Pre-Authorization_Code is not supported with Wallet_Initiated flow");
					}
					case ISSUER_INITIATED, ISSUER_INITIATED_DC_API -> prepareCredentialOffer();
				}
			}
		}

		setStatus(Status.WAITING);
	}

	protected void prepareCredentialOffer() {

		if (vciGrantType == VCIGrantType.PRE_AUTHORIZATION_CODE) {
			callAndStopOnFailure(VCIPreparePreAuthorizationCode.class, "OID4VCI-1FINAL-3.5", "OID4VCI-1FINAL-4.1");
		}

		callAndStopOnFailure(new VCICreateCredentialOffer(vciGrantType), "OID4VCI-1FINAL-4.1");

		if (vciCredentialOfferParameterVariantType == VCICredentialOfferParameterVariant.BY_REFERENCE) {
			callAndStopOnFailure(VCICreateCredentialOfferUri.class, "OID4VCI-1FINAL-4.1.3");
		}

		callAndStopOnFailure(new VCICreateCredentialOfferRedirectUrl(vciCredentialOfferParameterVariantType), "OID4VCI-1FINAL-4.1");
		browser.setShowQrCodes(true);

		if (vciAuthorizationCodeFlowVariant == VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED_DC_API) {
			callAndStopOnFailure(BuildVCIDCAPIRequest.class);
			JsonObject request = env.getObject("browser_api_request");
			browser.requestCredential(request, ""); // FIXME for now, no submitUrl === it's a VCI request, not VP
		} else {
			String credentialOfferRedirectUrl = env.getString("vci", "credential_offer_redirect_url");
			browser.goToUrl(credentialOfferRedirectUrl, null, "GET", 5);
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI2-SP-ID2-5.2.1-1", "FAPI2-SP-ID2-5.2.1-2");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		return handleClientRequestForPath(requestId, path);

	}


	protected Object handleClientRequestForPath(String requestId, String path) {
		if (path.equals("authorize")) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return authorizationEndpoint(requestId);
		} else if (path.equals("token")) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "This ecosystems requires that the token endpoint is called over an mTLS secured connection " +
					"using the token_endpoint found in mtls_endpoint_aliases.");
			} else {
				return tokenEndpoint(requestId);
			}
		} else if (path.equals("jwks")) {
			return jwksEndpoint();
		} else if (path.equals("userinfo")) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The userinfo endpoint must be called over an mTLS secured connection.");
			}
			return userinfoEndpoint(requestId);
		} else if (path.startsWith("statuslists")) {
			return statusListsEndpoints(requestId, path);
		} else if (path.equals(".well-known/openid-configuration")) {
			throw new TestFailureException(getId(), "The wallet has fetched .well-known/openid-configuration instead of .well-known/oauth-authorization-server as per RFC8414.");
		} else if (path.equals(".well-known/oauth-authorization-server")) {
			throw new TestFailureException(getId(), "The wallet has formed the path to .well-known/oauth-authorization-server using the OpenID Connect rules, but the .well-known rules from RFC8414 must be used.");
		} else if (path.equals(".well-known/openid-credential-issuer")) {
			throw new TestFailureException(getId(), "The wallet has formed the path to .well-known/openid-credential-issuer using the 'OpenID Connect rules', but since draft 16 of OID4VCI the .well-known rules from RFC8414 must be used.");
		} else if (path.equals("par")) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "In this ecosystem, the PAR endpoint must be called over an mTLS " +
					"secured connection using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases.");
			}
			if (clientAuthType == VCIClientAuthType.MTLS) {
				throw new TestFailureException(getId(), "The PAR endpoint must be called over an mTLS secured connection when using MTLS client authentication.");
			}
			return parEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH)) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}

			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The accounts endpoint must be called over an mTLS secured connection.");
			}

			return accountsEndpoint(requestId);
		} else if (path.equals(NONCE_PATH)) {
			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The nonce endpoint must be called over an mTLS secured connection.");
			}

			return nonceEndpoint(requestId);
		} else if (path.equals(CREDENTIAL_PATH)) {

			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The credentials endpoint must be called over an mTLS secured connection.");
			}

			return credentialEndpoint(requestId);
		} else if (path.startsWith(CREDENTIAL_OFFER_PATH)) {

			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The credential_offer endpoint must be called over an mTLS secured connection.");
			}

			return credentialOfferEndpoint(requestId, path);
		} else if (path.equals(DEFERRED_CREDENTIAL_PATH)) {

			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The deferred credential endpoint must be called over an mTLS secured connection.");
			}

			return deferredCredentialEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	protected Object statusListsEndpoints(String requestId, String path) {
		setStatus(Status.RUNNING);

		call(exec().mapKey("status_list_endpoint_request", requestId));

		ResponseEntity<Object> response;
		if (path.equals("statuslists") || path.equals("statuslists/")) {
			// status list aggregation endpoint
			// curl -k -H "Accept:application/statuslist+jwt" https://localhost.emobix.co.uk:8443/test/a/oidf-fapi-rp-test/statuslists

			// https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-9.3
			JsonObject aggreatedStatusList = new JsonObject();
			JsonArray statusLists = new JsonArray();
			// we only support one list for now
			String statusListUrl = getStatusListUrl("1");
			statusLists.add(statusListUrl);
			aggreatedStatusList.add("statuslists", statusLists);
			response = ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(aggreatedStatusList);
		} else {
			// status list endpoint
			// curl -k -H "Accept:application/statuslist+jwt" https://localhost.emobix.co.uk:8443/test/a/oidf-fapi-rp-test/statuslists/1

			// see https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-8.1
			String statusListId = path.substring(path.lastIndexOf("/") + 1);
			String statusListReference = "status_lists.status_list_" + statusListId;
			JsonElement statusListEl = env.getElementFromObject("vci", statusListReference);
			if (statusListEl == null) {
				response = ResponseEntity.notFound().build();
			} else {
				env.putString("current_status_list_id", statusListId);

				callAndContinueOnFailure(VCIGenerateJwtStatusListToken.class, ConditionResult.INFO, "OTSL-5.1");

				String currentStatusListJwt = env.getString("current_status_list_jwt");
				// TODO add cors headers
				// TODO handle time query parameter, see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-8.4
				response = ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/statuslist+jwt").body(currentStatusListJwt);
			}
		}

		call(exec().unmapKey("status_list_endpoint_request").endBlock());

		setStatus(Status.WAITING);
		return response;
	}

	protected String getStatusListUrl(String statusListId) {
		return env.getString("server", "issuer") + "statuslists/" + statusListId;
	}

	protected Object credentialOfferEndpoint(String requestId, String path) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Credential offer endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		env.getString("incoming_request", "request_url");

		ResponseEntity<Object> responseEntity;
		String credentialOfferId = path.substring(path.lastIndexOf("/") + 1);
		String expectedCredentialOfferId = env.getString("vci", "credential_offer_id");
		if (expectedCredentialOfferId.equals(credentialOfferId)) {
			JsonElement credentialOfferObject = env.getElementFromObject("vci", "credential_offer");
			responseEntity = ResponseEntity.status(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.body(credentialOfferObject);
		} else {
			responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);
		return responseEntity;
	}

	protected Object nonceEndpoint(String requestId) {
		// credential_issuer_nonce
		setStatus(Status.RUNNING);

		call(exec().startBlock("Nonce endpoint"));
		call(exec().mapKey("token_endpoint_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));
		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-7.2");
		callAndStopOnFailure(GenerateCredentialNonce.class, "OID4VCI-1FINAL-ID-7");
		callAndStopOnFailure(GenerateCredentialNonceResponse.class, "OID4VCI-1FINAL-7.2");
		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity;
		JsonObject nonceEndpointResponse = env.getObject("credential_nonce_response");
		JsonObject headerJson = env.getObject("credential_nonce_response_headers");
		responseEntity = new ResponseEntity<>(nonceEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);

		setStatus(Status.WAITING);
		return responseEntity;
	}

	@SuppressWarnings("unused")
	protected Object credentialEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Credential endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		checkResourceEndpointRequest(false);

		ResponseEntity<?> errorResponse;
		errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIValidateCredentialRequestStructure.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.2");
		if (errorResponse != null) {
			return errorResponse;
		}

		errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIResolveRequestedCredentialConfigurationFromRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-8.2");
		if (errorResponse != null) {
			return errorResponse;
		}

		// Check if the credential configuration requires cryptographic binding
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		boolean requiresCryptographicBinding = credentialConfiguration.has("cryptographic_binding_methods_supported");

		if (requiresCryptographicBinding) {
			// Only validate proofs if cryptographic binding is required
			errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIExtractCredentialRequestProof.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-F.4");
			if (errorResponse != null) {
				return errorResponse;
			}

			String proofType = env.getString("proof_type");
			if ("jwt".equals(proofType)) {
				errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIValidateCredentialRequestJwtProof.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.4");
			} else if ("attestation".equals(proofType)) {
				errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIValidateCredentialRequestAttestationProof.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-F.3", "OID4VCI-1FINALA-F.4", "HAIP-4.5.1");
			} else if ("di_vp".equals(proofType)) {
				errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIValidateCredentialRequestDiVpProof.class, ConditionResult.FAILURE, "OID4VCI-1FINALA-F.2", "OID4VCI-1FINALA-F.4");
			}

			if (errorResponse != null) {
				return errorResponse;
			}
		} else {
			eventLog.log(getName(), "Credential configuration does not require cryptographic binding, skipping proof validation");
		}

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.2.1");

		// Create the credential - this will be used for immediate or deferred response
		createCredential();

		HttpStatus responseStatus;
		if (vciCredentialIssuanceMode == VCICredentialIssuanceMode.DEFERRED) {
			// Deferred issuance: return transaction_id instead of credential
			// The credential is stored in the environment for retrieval at the deferred endpoint
			callAndStopOnFailure(VCICreateDeferredCredentialResponse.class, "OID4VCI-1FINAL-9");
			responseStatus = HttpStatus.ACCEPTED;
		} else {
			// Immediate issuance: return credential directly
			callAndStopOnFailure(VCICreateCredentialEndpointResponse.class,
				/* SD JWT VC */ "OID4VCI-1FINALA-A.3.4",
				/* modc */ "OID4VCI-1FINALA-A.2.4"
			);
			responseStatus = HttpStatus.OK;
		}

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		// Encrypt the response if wallet requested encryption
		ResponseEntity<?> encryptionErrorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIEncryptCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-10", "OID4VCI-1FINAL-8.3.1.2");
		if (encryptionErrorResponse != null) {
			return encryptionErrorResponse;
		}

		// Check if the response was encrypted
		String encryptedResponse = env.getString("encrypted_credential_response");
		if (encryptedResponse != null) {
			// Return encrypted response as application/jwt
			return createEncryptedCredentialEndpointResponse(encryptedResponse, responseStatus);
		}

		JsonObject credentialEndpointResponse = env.getObject("credential_endpoint_response");
		return createCredentialEndpointResponse(credentialEndpointResponse, responseStatus);
	}

	protected ResponseEntity<Object> createEncryptedCredentialEndpointResponse(String encryptedResponse, HttpStatus responseStatus) {
		call(exec().unmapKey("incoming_request").endBlock());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf("application/jwt"));

		if (requireResourceServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
		}

		resourceEndpointCallComplete();
		return new ResponseEntity<>(encryptedResponse, headers, responseStatus);
	}

	protected ResponseEntity<?> callAndContinueOnFailureOrReturnErrorResponse(Class<? extends AbstractCondition> conditionClass, ConditionResult conditionResult, String ... requirements) {
		callAndContinueOnFailure(conditionClass, conditionResult, requirements);
		JsonElement credentialErrorResponse = env.getElementFromObject("vci", "credential_error_response");
		if (credentialErrorResponse != null) {
			JsonObject credentialEndpointResponseBody = credentialErrorResponse.getAsJsonObject().getAsJsonObject("body");
			return createCredentialEndpointResponse(credentialEndpointResponseBody, HttpStatus.BAD_REQUEST);
		}
		return null;
	}

	protected ResponseEntity<Object> createCredentialEndpointResponse(JsonObject credentialEndpointResponse, HttpStatus responseStatus) {
		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			JsonObject headerJson = env.getObject("credential_endpoint_response_headers");

			if (requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}
			// at this point we can assume the test is fully done
			resourceEndpointCallComplete();
			responseEntity = new ResponseEntity<>(credentialEndpointResponse, headersFromJson(headerJson), responseStatus);
		}
		return responseEntity;
	}

	/**
	 * Handles the deferred credential endpoint per OID4VCI Section 9.
	 *
	 * When the wallet polls the deferred credential endpoint with a transaction_id,
	 * this endpoint validates the transaction_id and returns the credential that was
	 * created during the initial credential request.
	 *
	 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-9">OID4VCI Section 9 - Deferred Credential Issuance</a>
	 */
	protected Object deferredCredentialEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Deferred credential endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		checkResourceEndpointRequest(false);

		// Validate the deferred credential request (transaction_id)
		ResponseEntity<?> errorResponse;
		errorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIValidateDeferredCredentialRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-9.1");
		if (errorResponse != null) {
			return errorResponse;
		}

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");

		// Create the actual credential response (the credential was already generated during the initial request)
		callAndStopOnFailure(VCICreateCredentialEndpointResponse.class,
			/* SD JWT VC */ "OID4VCI-1FINALA-A.3.4",
			/* mdoc */ "OID4VCI-1FINALA-A.2.4"
		);

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		// Encrypt the response if wallet requested encryption
		ResponseEntity<?> encryptionErrorResponse = callAndContinueOnFailureOrReturnErrorResponse(VCIEncryptCredentialResponse.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-10", "OID4VCI-1FINAL-8.3.1.2");
		if (encryptionErrorResponse != null) {
			return encryptionErrorResponse;
		}

		// Check if the response was encrypted
		String encryptedResponse = env.getString("encrypted_credential_response");
		if (encryptedResponse != null) {
			// Return encrypted response as application/jwt
			return createEncryptedCredentialEndpointResponse(encryptedResponse, HttpStatus.OK);
		}

		JsonObject credentialEndpointResponse = env.getObject("credential_endpoint_response");
		return createCredentialEndpointResponse(credentialEndpointResponse, HttpStatus.OK);
	}

	protected void createCredential() {

		// Determine format from the resolved credential_configuration (set by VCIResolveRequestedCredentialConfigurationFromRequest)
		String requestedFormat = env.getString("credential_configuration", "format");

		if ("mso_mdoc".equals(requestedFormat)) {
			// mdoc format - the doctype is in credential_configuration.doctype
			callAndStopOnFailure(CreateMdocCredentialForVCI.class, "OID4VCI-1FINALA-G.1");
		} else {
			// SD-JWT VC format (dc+sd-jwt or default)
			if (vciProfile == VCIProfile.HAIP) {
				Map<String, Object> additionalClaims = additionalSdJwtClaimsForHaip();
				callAndStopOnFailure(new CreateSdJwtCredential(additionalClaims), "OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.3");
			} else {
				callAndStopOnFailure(CreateSdJwtCredential.class, "OID4VCI-1FINALA-F.1", "OID4VCI-1FINALA-F.3");
			}
		}
	}

	protected Map<String, Object> additionalSdJwtClaimsForHaip() {

		Map<Object, Object> statusList = generateStatusListEntryForSdJwtStatusClaim();

		Map<Object, Object> status = new HashMap<>();
		status.put("status_list", statusList);

		Map<String, Object> additionalClaims = new HashMap<>();
		additionalClaims.put("status", status);
		return additionalClaims;
	}

	protected Map<Object, Object> generateStatusListEntryForSdJwtStatusClaim() {
		Map<Object, Object> statusList = new HashMap<>();
		statusList.put("idx", 0); // even indices indicate Status.VALID, odd indices indicate Status.INVALID, see: VCIGenerateJwtStatusListToken
		statusList.put("uri", getStatusListUrl("1"));
		return statusList;
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI2-SP-ID2-5.2.1-1", "FAPI2-SP-ID2-5.2.1-2");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH) || path.equals(FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH)) {
			if (!isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The accounts endpoint must not be called over an mTLS secured connection.");
			}

			return accountsEndpoint(requestId);
		} else if (path.equals(CREDENTIAL_PATH)) {
			if (!isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The credential endpoint must not be called over an mTLS secured connection.");
			}

			return credentialEndpoint(requestId);
		} else if (path.equals(NONCE_PATH)) {
			if (!isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The nonce endpoint must be called over an mTLS secured connection.");
			}

			return nonceEndpoint(requestId);
		} else if (path.equals(DEFERRED_CREDENTIAL_PATH)) {
			if (!isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The deferred credential endpoint must not be called over an mTLS secured connection.");
			}

			return deferredCredentialEndpoint(requestId);
		} else if (path.equals("userinfo")) {
			if (startingShutdown) {
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return userinfoEndpoint(requestId);
		} else if (path.equals("par")) {
			return parEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP (using mtls) call to " + path);
	}

	@Override
	public Object handleWellKnown(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		call(exec().startBlock("Get OAuth Authorization Metadata").mapKey("incoming_request", requestId));
		Object response;

		if (path.startsWith("/.well-known/oauth-authorization-server")) {
			response = discoveryEndpoint();
		} else if (path.startsWith("/.well-known/openid-credential-issuer")) {
			response = credentialIssuerMetadataEndpoint(requestId);
		} else {
			response = super.handleWellKnown(path, req, res, session, requestParts);
		}
		call(exec().unmapKey("incoming_request").endBlock());
		return response;
	}

	protected void validateResourceEndpointHeaders() {
		// FIXME: No obvious FAPI2 equivalent
		skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-3");

		// FIXME: No obvious FAPI2 equivalent
		skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address", ConditionResult.INFO,
			ExtractFapiIpAddressHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-4");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");
		callAndContinueOnFailure(ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

	}

	protected void checkResourceEndpointRequest(boolean useClientCredentialsAccessToken) {
		senderConstrainTokenRequestHelper.checkResourceRequest();
		if (useClientCredentialsAccessToken) {
			call(sequence(validateSenderConstrainedClientCredentialAccessTokenSteps));
		} else {
			call(sequence(validateSenderConstrainedTokenSteps));
		}
		validateResourceEndpointHeaders();
	}

	protected Object brazilHandleNewConsentRequest(String requestId, boolean isPayments) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("New consent endpoint").mapKey("incoming_request", requestId));
		env.putBoolean("payments_consent_endpoint_called", isPayments);
		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		//Requires method=POST. defined in API docs
		callAndStopOnFailure(EnsureIncomingRequestMethodIsPost.class);

		checkResourceEndpointRequest(true);

		if (isPayments) {
			callAndStopOnFailure(FAPIBrazilExtractCertificateSubjectFromServerJwks.class);
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedPayments.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractPaymentsConsentRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);
			//ensure aud equals endpoint url	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, ConditionResult.FAILURE, "RFC7519-4.1.3", "BrazilOB-6.1");
			//ensure ISS equals TLS certificate organizational unit
			callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			//ensure jti is uuid	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestJtiIsUUIDv4.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			callAndContinueOnFailure(FAPIBrazilValidateConsentRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1");

			callAndContinueOnFailure(FAPIBrazilFetchClientOrganizationJwksFromDirectory.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			env.mapKey("parsed_client_request_jwt", "new_consent_request");
			callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			env.unmapKey("parsed_client_request_jwt");

		} else {
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedConsents.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
		}

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if (isPayments) {
				callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentsConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-2");
				String signedConsentResponse = env.getString("signed_consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");

				HttpHeaders headers = headersFromJson(headerJson);
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
				responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);
			} else {
				callAndContinueOnFailure(FAPIBrazilGenerateNewConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				JsonObject response = env.getObject("consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");
				responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.CREATED);
			}
			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
		}

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return responseEntity;
	}


	protected Object brazilHandleGetConsentRequest(String requestId, String path, boolean isPayments) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Get consent endpoint").mapKey("incoming_request", requestId));
		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));


		checkResourceEndpointRequest(true);
		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		String requestedConsentId = path.substring(path.lastIndexOf('/') + 1);
		env.putString("requested_consent_id", requestedConsentId);

		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if (isPayments) {
				callAndContinueOnFailure(FAPIBrazilGenerateGetPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
				callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
				String signedConsentResponse = env.getString("signed_consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");

				HttpHeaders headers = headersFromJson(headerJson);
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
				responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.OK);

			} else {
				callAndContinueOnFailure(FAPIBrazilGenerateGetConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				JsonObject response = env.getObject("consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");
				responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.OK);
			}

			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
		}

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return responseEntity;
	}

	protected Object brazilHandleNewPaymentInitiationRequest(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		call(exec().startBlock("Payment initiation endpoint").mapKey("incoming_request", requestId));
		//Requires method=POST. defined in API docs
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE);

		checkResourceEndpointRequest(false);

		callAndContinueOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainPayments.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(FAPIBrazilExtractPaymentInitiationRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
		env.mapKey("parsed_client_request_jwt", "payment_initiation_request");
		callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		env.unmapKey("parsed_client_request_jwt");

		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);

		//ensure aud equals endpoint url	"BrazilOB-6.1"
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestAud.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		//ensure ISS equals TLS certificate organizational unit
		callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1");


		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			callAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			String signedConsentResponse = env.getString("signed_payment_initiation_response");
			JsonObject headerJson = env.getObject("payment_initiation_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
			resourceEndpointCallComplete();
		}

		call(exec().unmapKey("incoming_request").endBlock());
		return responseEntity;
	}

	protected void resourceEndpointCallComplete() {
		// at this point we can assume the test is fully done

		setStatus(Status.WAITING);

		int waitTimeSeconds = 40;
		eventLog.log(getId(), """
			Detected completed credential endpoint call. Waiting %d seconds for additional wallet requests before completing the test."""
			.formatted(waitTimeSeconds));

		getTestExecutionManager().scheduleInBackground(() -> {
			setStatus(Status.RUNNING);
			fireTestFinished();
			return null;
		}, waitTimeSeconds, TimeUnit.SECONDS);

	}

	protected Object discoveryEndpoint() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(EnsureIncomingRequestMethodIsGet.class, ConditionResult.FAILURE, "RFC8414-3.1");
		callAndContinueOnFailure(VCICheckOAuthAuthorizationServerMetadataRequestUrl.class, ConditionResult.FAILURE, "RFC8414-3.1");

		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	protected Object credentialIssuerMetadataEndpoint(String requestId) {

		JsonElement acceptHeader = env.getElementFromObject(requestId, "headers.accept");

		setStatus(Status.RUNNING);
		ResponseEntity<?> responseEntity;
		if (acceptHeader != null && OIDFJSON.getString(acceptHeader).equalsIgnoreCase("application/jwt")) {
			generateSignedCredentialIssuerMetadata();
			String signedCredentialIssuerMetadata = env.getString("signed_credential_issuer_metadata");
			responseEntity = ResponseEntity.status(HttpStatus.OK).contentType(MediaType.parseMediaType("application/jwt")).body(signedCredentialIssuerMetadata);
		} else {
			JsonObject credentialIssuerMetadata = env.getObject("credential_issuer_metadata");
			responseEntity = ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(credentialIssuerMetadata);
		}

		callAndContinueOnFailure(EnsureIncomingRequestMethodIsGet.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-11.2.2");
		callAndContinueOnFailure(VCICheckIssuerMetadataRequestUrl.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-11.2.2");

		setStatus(Status.WAITING);
		return responseEntity;
	}

	protected void checkMtlsCertificate() {
		callAndContinueOnFailure(ExtractClientCertificateFromRequestHeaders.class, ConditionResult.FAILURE);
		callAndStopOnFailure(CheckForClientCertificate.class, ConditionResult.FAILURE, "FAPI2-SP-ID2-5.3.1.1-4");
		callAndContinueOnFailure(EnsureClientCertificateMatches.class, ConditionResult.FAILURE);
	}

	private abstract static class SenderContrainTokenRequestHelper {
		public abstract void checkParRequest();

		public abstract void checkTokenRequest();

		public abstract void checkResourceRequest();
	}

	private class DPopTokenRequestHelper extends SenderContrainTokenRequestHelper {
		@Override
		public void checkParRequest() {
			env.removeObject("incoming_dpop_proof");
			skipIfElementMissing("incoming_request", "headers.dpop", ConditionResult.INFO, ExtractDpopProofFromHeader.class, ConditionResult.FAILURE, "DPOP-5");
			if (env.containsObject("incoming_dpop_proof")) {
				call(sequence(PerformDpopProofParRequestChecks.class));
			}
		}

		@Override
		public void checkTokenRequest() {
			callAndStopOnFailure(ExtractDpopProofFromHeader.class, "DPOP-5");
			call(sequence(PerformDpopProofTokenRequestChecks.class));
		}

		@Override
		public void checkResourceRequest() {
			callAndStopOnFailure(ExtractDpopProofFromHeader.class, "DPOP-5");
			// Need to also extract the DPoP Access token for resource requests
			callAndStopOnFailure(ExtractDpopAccessTokenFromHeader.class, "DPOP-7");
			call(sequence(PerformDpopProofResourceRequestChecks.class));
		}
	}

	private class MtlsTokenRequestHelper extends SenderContrainTokenRequestHelper {
		@Override
		public void checkParRequest() {

		}

		@Override
		public void checkTokenRequest() {
		}

		@Override
		public void checkResourceRequest() {
			callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI2-SP-ID2-5.3.3-2");
			callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI2-SP-ID2-5.3.3-2");
		}
	}

	protected void authenticateParEndpointRequest(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));

		if (clientAuthType == VCIClientAuthType.MTLS || profileRequiresMtlsEverywhere) {
			// there is generally no requirement to present an MTLS certificate at the PAR endpoint when using private_key_jwt.
			// (This differs to the token endpoint, where an MTLS certificate must always be presented, as one is
			// required to bind the issued access token to.)
			checkMtlsCertificate();
		}

		if (clientAuthType == VCIClientAuthType.PRIVATE_KEY_JWT) {
			call(new ValidateClientAuthenticationWithPrivateKeyJWT().
				replace(ValidateClientAssertionClaims.class, condition(ValidateClientAssertionClaimsForPAREndpoint.class).requirements("PAR-2")).then(condition(ValidateClientAssertionAudClaimIsIssuerAsString.class).onFail(ConditionResult.FAILURE).requirements("FAPI2-SP-ID2-5.3.2.1-5").dontStopOnFailure())
			);
		} else {
			call(sequence(validateClientAuthenticationSteps));
		}
		call(exec().unmapKey("token_endpoint_request"));
	}

	protected void extractParEndpointRequest() {
		skipIfElementMissing("par_endpoint_http_request", "body_form_params.request", ConditionResult.INFO, ExtractRequestObjectFromPAREndpointRequest.class, ConditionResult.FAILURE, "PAR-3");
		callAndStopOnFailure(EnsurePAREndpointRequestDoesNotContainRequestUriParameter.class, "PAR-2.1");
		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
	}

	protected Object parEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("PAR endpoint").mapKey("par_endpoint_http_request", requestId)
			.mapKey("incoming_request", requestId));

		try {
			authenticateParEndpointRequest(requestId);
		} catch (ConditionError | TestFailureException e) {
			// Client authentication failed - return invalid_client error
			String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
			env.putString("par_endpoint_client_auth_error_description", errorMessage);
			callAndContinueOnFailure(CreatePAREndpointInvalidClientErrorResponse.class, ConditionResult.INFO);
			ResponseEntity<Object> errorResponse = new ResponseEntity<>(
				env.getObject("par_endpoint_response"),
				HttpStatus.valueOf(env.getInteger("par_endpoint_response_http_status")));
			setStatus(Status.WAITING);
			call(exec().unmapKey("incoming_request").unmapKey("par_endpoint_http_request"));
			return errorResponse;
		}

		setParAuthorizationEndpointRequestParamsForHttpMethod();
		extractParEndpointRequest();

		if (env.containsObject("authorization_request_object")) {
			validateRequestObjectForPAREndpointRequest();
		}
		senderConstrainTokenRequestHelper.checkParRequest();
		if (isDpopConstrain()) {
			callAndContinueOnFailure(ExtractParAuthorizationCodeDpopBindingKey.class, ConditionResult.FAILURE, "DPOP-10");
		}

		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("par_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreatePAREndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("par_endpoint_response"), headersFromJson(env.getObject("par_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("par_endpoint_response_http_status").intValue()));
		} else {
			JsonObject parResponse = createPAREndpointResponse();
			responseEntity = new ResponseEntity<>(parResponse, HttpStatus.CREATED);
		}

		setStatus(Status.WAITING);
		call(exec().unmapKey("incoming_request").unmapKey("par_endpoint_http_request"));

		return responseEntity;
	}

	protected void addCustomValuesToParResponse() {
	}

	protected JsonObject createPAREndpointResponse() {
		callAndStopOnFailure(CreatePAREndpointResponse.class, "PAR-2.2");
		addCustomValuesToParResponse();
		JsonObject parResponse = env.getObject("par_endpoint_response");
		return parResponse;
	}

	protected Object userinfoEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint")
			.mapKey("incoming_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			call(exec().mapKey("token_endpoint_request", requestId));
			checkMtlsCertificate();
			call(exec().unmapKey("token_endpoint_request"));
		}

		checkResourceEndpointRequest(false);

		callAndStopOnFailure(FilterUserInfoForScopes.class);

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if (requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}

			setStatus(Status.WAITING);

			responseEntity = new ResponseEntity<>(user, HttpStatus.OK);
		}
		return responseEntity;
	}

	protected Object jwksEndpoint() {
		return jwksEndpoint("server_public_jwks");
	}

	protected Object jwksEndpoint(String jwksReference) {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject(jwksReference);

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object tokenEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint")
			.mapKey("token_endpoint_request", requestId));

		if (isDpopConstrain()) {
			call(exec().mapKey("incoming_request", requestId));
		}

		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		if (clientAuthType == VCIClientAuthType.MTLS || isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		if (clientAuthType == VCIClientAuthType.PRIVATE_KEY_JWT) {
			call(new ValidateClientAuthenticationWithPrivateKeyJWT().
				then(condition(ValidateClientAssertionAudClaimIsIssuerAsString.class).onFail(ConditionResult.FAILURE).requirements("FAPI2-SP-ID2-5.3.2.1-5").dontStopOnFailure())
			);
		} else {
			call(sequence(validateClientAuthenticationSteps));
		}

		if (vciGrantType == VCIGrantType.PRE_AUTHORIZATION_CODE) {
			callAndStopOnFailure(VCIValidateTxCode.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-3.5");
		}

		Object tokenResponseOb = handleTokenEndpointGrantType(requestId);
		if (isDpopConstrain()) {
			call(exec().unmapKey("incoming_request"));
		}
		return tokenResponseOb;

	}

	protected Object handleTokenEndpointGrantType(String requestId) {

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw new TestFailureException(getId(), "Token endpoint body does not contain the mandatory 'grant_type' parameter");
		}

		switch (grantType) {
			case "authorization_code":
				// we're doing the authorization code grant for user access
				return authorizationCodeGrantType(requestId);
			case "client_credentials":
				break;
			case "refresh_token":
				return refreshTokenGrantType(requestId);
			case "urn:ietf:params:oauth:grant-type:pre-authorized_code":
				return preAuthorizationCodeGrantType();
		}
		throw new TestFailureException(getId(), "Got an unexpected grant type on the token endpoint: " + grantType);
	}

	private Object preAuthorizationCodeGrantType() {

		senderConstrainTokenRequestHelper.checkTokenRequest();

		ResponseEntity<Object> responseObject;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {
			callAndStopOnFailure(VCIValidatePreAuthorizationCode.class);

			// FIXME is this needed for pre-authorization_code flow?
			// call(sequence(CheckPkceCodeVerifier.class));

			injectCredentialConfigurationDetailsIntoRequestContextForPreAuthorizedCodeFlow();

			issueAccessToken();

			issueRefreshToken();

			String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
			if ("yes".equals(isOpenIdScopeRequested)) {
				issueIdToken(false);
			}

			createTokenEndpointResponse();
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);

			// Create a new DPoP nonce
			if (requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);
		return responseObject;
	}

	protected void injectCredentialConfigurationDetailsIntoRequestContextForPreAuthorizedCodeFlow() {
		if (authorizationRequestType == AuthorizationRequestType.RAR) {
			callAndStopOnFailure(VCIInjectAuthorizationDetailsForPreAuthorizedCodeFlow.class, ConditionResult.FAILURE);
		}
	}

	protected Object refreshTokenGrantType(String requestId) {
		senderConstrainTokenRequestHelper.checkTokenRequest();
		ResponseEntity<Object> responseObject = null;

		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {
			callAndStopOnFailure(ValidateRefreshToken.class);

			issueAccessToken();
			issueRefreshToken(); // rotate refresh token
			env.removeNativeValue("id_token");
			callAndStopOnFailure(CreateTokenEndpointResponse.class);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);

			// Create a new DPoP nonce
			if (requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return responseObject;
	}

	protected Object clientCredentialsGrantType(String requestId) {

		senderConstrainTokenRequestHelper.checkTokenRequest();
		ResponseEntity<Object> responseObject = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {

			callAndStopOnFailure(generateSenderConstrainedAccessToken);

			callAndStopOnFailure(CreateTokenEndpointResponse.class);

			// this puts the client credentials specific token into its own box for later
			if (isMTLSConstrain()) {
				callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);
			} else {
				callAndStopOnFailure(CopyAccessTokenToDpopClientCredentialsField.class);
			}
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);
			// Create a new DPoP nonce
			if (requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return responseObject;
	}

	protected void validateRedirectUriForAuthorizationCodeGrantType() {
		callAndContinueOnFailure(ValidateRedirectUri.class, ConditionResult.FAILURE);
	}

	protected Object authorizationCodeGrantType(String requestId) {
		senderConstrainTokenRequestHelper.checkTokenRequest();

		ResponseEntity<Object> responseObject = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {
			callAndStopOnFailure(ValidateAuthorizationCode.class);

			validateRedirectUriForAuthorizationCodeGrantType();

			call(sequence(CheckPkceCodeVerifier.class));

			issueAccessToken();

			issueRefreshToken();

			String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
			if ("yes".equals(isOpenIdScopeRequested)) {
				issueIdToken(false);
			}

			createTokenEndpointResponse();
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);

			// Create a new DPoP nonce
			if (requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);
		return responseObject;
	}

	protected void createTokenEndpointResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		callAndStopOnFailure(VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse.class);

		if (authorizationRequestType == AuthorizationRequestType.RAR) {
			callAndStopOnFailure(RARSupport.AddRarToTokenEndpointResponse.class);
		}
	}

	protected void setAuthorizationEndpointRequestParamsForHttpMethod() {
		String httpMethod = env.getString("authorization_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("authorization_endpoint_http_request");
		if ("POST".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else if ("GET".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("query_string_params"));
		} else {
			//this should not happen?
			throw new TestFailureException(getId(), "Got unexpected HTTP method to authorization endpoint");
		}
	}

	/**
	 * Saves the PAR authorization request params in case it's unsigned/unencrypted
	 */
	protected void setParAuthorizationEndpointRequestParamsForHttpMethod() {
		String httpMethod = env.getString("par_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("par_endpoint_http_request");
		if ("POST".equals(httpMethod)) {
			env.putObject("par_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP method '" + httpMethod + "' to par endpoint");
		}
	}

	@UserFacing
	protected Object authorizationEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();
		callAndStopOnFailure(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR.class);
		callAndStopOnFailure(EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR.class);

		if (vciGrantType == VCIGrantType.AUTHORIZATION_CODE) {
			if (vciAuthorizationCodeFlowVariant == VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED ||
				vciAuthorizationCodeFlowVariant == VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED_DC_API) {
				callAndStopOnFailure(VCIVerifyIssuerStateInAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VCI-1FINAL-5.1.3");
			}
		}

		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationPARRequestParameters.class);
		if (authorizationRequestType == AuthorizationRequestType.RAR) {
			callAndStopOnFailure(EnsureEffectiveAuthorizationEndpointRequestContainsValidRAR.class, ConditionResult.FAILURE, "RAR-2.0");
		}

		endTestIfRequiredParametersAreMissing();
		callAndStopOnFailure(EnsureResponseTypeIsCode.class, "FAPI2-SP-ID2-5.3.1.2-1");

		skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInRequestObject.class, ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		callAndStopOnFailure(EnsureAuthorizationRequestContainsPkceCodeChallenge.class, "FAPI2-SP-ID2-5.3.2.2-3");
		validateRequestObjectForAuthorizationEndpointRequest();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if ("yes".equals(isOpenIdScopeRequested)) {
			throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
		}
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE, ConditionResult.INFO, EnsureAuthorizationRequestContainsStateParameter.class, ConditionResult.FAILURE, "RFC6749-4.1.1");

		createAuthorizationEndpointResponse();

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		if (requireAuthorizationServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
		}
		setStatus(Status.WAITING);

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return new RedirectView(redirectTo, false, false, false);

	}

	/**
	 * Common checks applicable to both PAR endpoint and authorization requests
	 */
	protected void validateRequestObjectCommonChecks() {
		callAndStopOnFailure(FAPI2ValidateRequestObjectSigningAlg.class, "FAPI2-SP-ID2-5.4");
		callAndContinueOnFailure(FAPIValidateRequestObjectMediaType.class, ConditionResult.WARNING, "JAR-4");
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI2-MS-ID1-5.3.1-4");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI2-MS-ID1-5.3.1-3");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.FAILURE, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.FAILURE, "JAR-10.8");
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI2-MS-ID1-5.3.1-1");
		validateRedirectUriInRequestObject();
	}

	protected void validateRedirectUriInRequestObject() {
		callAndContinueOnFailure(EnsureMatchingRedirectUriInRequestObject.class, ConditionResult.FAILURE);
	}

	protected void validateRequestObjectForAuthorizationEndpointRequest() {
		if (fapi2AuthRequestMethod == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION) {
			callAndContinueOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.FAILURE,
				"FAPI2-MS-ID1-5.3.2-1");
		}

		if (authorizationRequestType == AuthorizationRequestType.SIMPLE) {
			// only check scopes if we expect scopes
			callAndStopOnFailure(ExtractRequestedScopes.class);
		}
		checkRequestedScopes();

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");
	}

	protected void checkRequestedScopes() {
		// TODO check for scopes?
		// callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScope.class);
	}

	protected void validateRequestObjectForPAREndpointRequest() {
		validateRequestObjectCommonChecks();
		callAndStopOnFailure(EnsureRequestObjectContainsCodeChallengeWhenUsingPAR.class, "FAPI2-SP-ID2-5.3.1.2-5");
	}

	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		prepareIdTokenClaims(isAuthorizationEndpoint);

		signIdToken();

		encryptIdToken(isAuthorizationEndpoint);
	}

	protected void issueAccessToken() {
		callAndStopOnFailure(generateSenderConstrainedAccessToken);
		callAndContinueOnFailure(GenerateAccessTokenExpiration.class, ConditionResult.INFO);
		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");
	}

	protected void issueRefreshToken() {
		callAndStopOnFailure(CreateRefreshToken.class);
	}

	protected void prepareIdTokenClaims(boolean isAuthorizationEndpoint) {

		//3.3.3.6 The at_hash and c_hash Claims MAY be omitted from the ID Token returned from the Token Endpoint even when these Claims are present in the ID Token returned from the Authorization Endpoint,
		//TODO skip or add?
		if (isAuthorizationEndpoint) {
			callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");
			// FIXME = No obvkous FAPI2 equivalent. PKCE replaces s_hash
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE,
				ConditionResult.INFO, CalculateSHash.class, ConditionResult.FAILURE);
		}

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		if (!isAuthorizationEndpoint && authorizationCodeGrantTypeProfileSteps != null) {
			call(sequence(authorizationCodeGrantTypeProfileSteps));
		}

		if (isAuthorizationEndpoint && authorizationEndpointProfileSteps != null) {
			call(sequence(authorizationEndpointProfileSteps));
		}

		//TODO skip or add?
		if (isAuthorizationEndpoint) {
			callAndStopOnFailure(AddCHashToIdTokenClaims.class, "OIDCC-3.3.2.11");
			skipIfMissing(null, new String[]{"s_hash"}, ConditionResult.INFO,
				AddSHashToIdTokenClaims.class, ConditionResult.FAILURE);
		}
		skipIfMissing(null, new String[]{"at_hash"}, ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
			AddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
	}

	protected void signIdToken() {
		callAndStopOnFailure(SignIdToken.class);

		addCustomSignatureOfIdToken();
	}

	/**
	 * This method does not actually encrypt id_tokens, even when id_token_encrypted_response_alg is set
	 * "5.2.3.1.  ID Token as detached signature" reads:
	 * "5. shall support both signed and signed & encrypted ID Tokens."
	 * So an implementation MUST support non-encrypted id_tokens too and we do NOT allow testers to run all tests with id_token
	 * encryption enabled, encryption will be enabled only for certain tests and the rest will return non-encrypted id_tokens.
	 * Second client will be used for encrypted id_token tests. First client does not need to have an encryption key
	 *
	 * @param isAuthorizationEndpoint
	 */
	protected void encryptIdToken(boolean isAuthorizationEndpoint) {
	}

	protected void createAuthorizationEndpointResponse() {
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		callAndStopOnFailure(AddIssToAuthorizationEndpointResponseParams.class, "FAPI2-SP-ID2-5.3.1.2-7");

		addCustomValuesToAuthorizationResponse();

		callAndStopOnFailure(SendAuthorizationResponseWithResponseModeQuery.class, "OIDCC-3.1.2.5");

		exposeEnvString("authorization_endpoint_response_redirect");
	}

	protected void addCustomValuesToJarmResponse() {
	}

	protected void generateJARMResponseClaims() {
		callAndStopOnFailure(GenerateJARMResponseClaims.class, "JARM-2.1.1");
		addCustomValuesToJarmResponse();
	}

	/**
	 * OpenBanking account request API
	 *
	 * @param requestId
	 * @return
	 */
	protected Object accountRequestsEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Account request endpoint")
			.mapKey("incoming_request", requestId));

		checkResourceEndpointRequest(true);

		ResponseEntity<Object> responseObject = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			// TODO: should we clear the old headers?
			callAndStopOnFailure(GenerateAccountRequestId.class);
			exposeEnvString("account_request_id");

			callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");

			callAndStopOnFailure(CreateOpenBankingAccountRequestResponse.class);

			JsonObject accountRequestResponse = env.getObject("account_request_response");
			JsonObject headerJson = env.getObject("account_request_response_headers");

			callAndStopOnFailure(ClearAccessTokenFromRequest.class);
			responseObject = new ResponseEntity<>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);
			if (requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}
		}
		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return responseObject;
	}

	protected Object accountsEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Accounts endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		if (isMTLSConstrain() || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		checkResourceEndpointRequest(false);

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		if (accountsEndpointProfileSteps != null) {
			call(sequence(accountsEndpointProfileSteps));
		}

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity = null;
		if (isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			JsonObject accountsEndpointResponse = env.getObject("accounts_endpoint_response");
			JsonObject headerJson = env.getObject("accounts_endpoint_response_headers");

			if (requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}
			// at this point we can assume the test is fully done
			resourceEndpointCallComplete();
			responseEntity = new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
		}
		return responseEntity;
	}

	@VariantSetup(parameter = VCIClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointAuthMethodSupported = AddTLSClientAuthToServerConfiguration.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithMTLS.class;
	}

	@VariantSetup(parameter = VCIClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	@VariantSetup(parameter = VCIClientAuthType.class, value = "client_attestation")
	public void setupClientAttestation() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToAttestJwtClientAuthOnly.class;
		validateClientAuthenticationSteps = VCIValidateClientAuthenticationWithClientAttestationJWT.class;
	}

	public void setupPlainFapi() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	public void setupResponseModePlain() {
		configureResponseModeSteps = null;
	}

	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "mtls")
	public void setupSenderConstrainMethodMTLS() {
		generateSenderConstrainedAccessToken = GenerateBearerAccessToken.class;
		validateSenderConstrainedTokenSteps = RequireMtlsAccessToken.class;
		validateSenderConstrainedClientCredentialAccessTokenSteps = RequireMtlsClientCredentialsAccessToken.class;
		senderConstrainTokenRequestHelper = new MtlsTokenRequestHelper();
	}

	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupSenderConstrainMethodDPop() {
		generateSenderConstrainedAccessToken = GenerateDpopAccessToken.class;
		validateSenderConstrainedTokenSteps = RequireDpopAccessToken.class;
		validateSenderConstrainedClientCredentialAccessTokenSteps = RequireDpopClientCredentialAccessToken.class;
		senderConstrainTokenRequestHelper = new DPopTokenRequestHelper();
	}

	protected void startWaitingForTimeout() {
		this.startingShutdown = true;
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(waitTimeoutSeconds * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the client hasn't called the token endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";

		});
	}
}
