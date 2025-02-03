package net.openid.conformance.fapi2spid2;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeChallengeMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddDpopSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddIdTokenSigningAlgsToServerConfiguration;
import net.openid.conformance.condition.as.AddIssSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddIssToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddResponseTypeCodeToServerConfiguration;
import net.openid.conformance.condition.as.AddSHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddScopesSupportedOpenIdToServerConfiguration;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedPairwiseToServerConfiguration;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddSupportedAuthorizationTypesToServerConfiguration;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdAddClaimsSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject;
import net.openid.conformance.condition.as.AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims;
import net.openid.conformance.condition.as.AustraliaConnectIdValidatePurpose;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CalculateSHash;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInClaimsParameter;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInRequestObject;
import net.openid.conformance.condition.as.CheckForUnexpectedOpenIdClaims;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterMemberValues;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterValues;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CopyAccessTokenToDpopClientCredentialsField;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateAuthorizationServerDpopNonce;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreatePAREndpointDpopErrorResponse;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.CreateTokenEndpointDpopErrorResponse;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EncryptJARMResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsStateParameter;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureMatchingRedirectUriInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsurePAREndpointRequestDoesNotContainRequestUriParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequestedScopeIsEqualToConfiguredScope;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.EnsureScopeContainsAccounts;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractParAuthorizationCodeDpopBindingKey;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPI2ValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentInitiationRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentsConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetGrantTypesSupportedInServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentConsentResponse;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentInitiationResponse;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectExp;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectMediaType;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateDpopAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateServerConfigurationMTLS;
import net.openid.conformance.condition.as.LoadRequestedIdTokenClaims;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SetRsaAltServerJwks;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
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
import net.openid.conformance.condition.as.jarm.SendJARMResponseWitResponseModeQuery;
import net.openid.conformance.condition.as.jarm.SignJARMResponse;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR;
import net.openid.conformance.condition.as.par.ExtractRequestObjectFromPAREndpointRequest;
import net.openid.conformance.condition.client.AugmentRealJwksWithDecoys;
import net.openid.conformance.condition.client.AustraliaConnectIdEnsureAuthorizationRequestContainsNoAcrClaims;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse;
import net.openid.conformance.condition.rs.CreateOpenBankingAccountRequestResponse;
import net.openid.conformance.condition.rs.CreateResourceEndpointDpopErrorResponse;
import net.openid.conformance.condition.rs.CreateResourceServerDpopNonce;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractDpopAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractDpopProofFromHeader;
import net.openid.conformance.condition.rs.ExtractFapiDateHeader;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader;
import net.openid.conformance.condition.rs.ExtractXIdempotencyKeyHeader;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts;
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
import net.openid.conformance.sequence.as.AddJARMToServerConfiguration;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationCodeGrant;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationEndpointResponse;
import net.openid.conformance.sequence.as.AddPARToServerConfiguration;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.sequence.as.GenerateOpenBankingUkAccountsEndpointResponse;
import net.openid.conformance.sequence.as.PerformDpopProofParRequestChecks;
import net.openid.conformance.sequence.as.PerformDpopProofResourceRequestChecks;
import net.openid.conformance.sequence.as.PerformDpopProofTokenRequestChecks;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.AuthorizationRequestType;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

@VariantParameters({
	ClientAuthType.class,
	FAPI2ID2OPProfile.class,
	FAPIResponseMode.class,
	FAPIClientType.class,
	FAPI2AuthRequestMethod.class,
	FAPI2SenderConstrainMethod.class,
		AuthorizationRequestType.class,
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantConfigurationFields(parameter = FAPI2ID2OPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.keystore"
})
@VariantHidesConfigurationFields(parameter = FAPIResponseMode.class, value = "jarm", configurationFields = {
	"client2.client_id",
	"client2.scope",
	"client2.redirect_uri",
	"client2.certificate",
	"client2.jwks",
	"client2.id_token_encrypted_response_alg",
	"client2.id_token_encrypted_response_enc",
})
@VariantHidesConfigurationFields(parameter = FAPI2ID2OPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.scope",
	"client2.scope"
})
@VariantHidesConfigurationFields(parameter = FAPI2ID2OPProfile.class, value = "connectid_au", configurationFields = {
	"client.scope", // scope is always openid
	"client2.scope"
})
@VariantConfigurationFields(parameter = AuthorizationRequestType.class, value = "rar", configurationFields = {
		"resource.authorization_details_types_supported"
})
public abstract class AbstractFAPI2SPID2ClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";
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

	// Controls which endpoints we should expose to the client
	protected FAPI2ID2OPProfile profile;

	protected FAPIResponseMode responseMode;

	protected ClientAuthType clientAuthType;

	protected FAPIClientType fapiClientType;

	protected FAPI2SenderConstrainMethod fapi2SenderConstrainMethod;

	protected FAPI2AuthRequestMethod fapi2AuthRequestMethod;

	protected AuthorizationRequestType authorizationRequestType;

	protected boolean startingShutdown = false;

	protected Boolean profileRequiresMtlsEverywhere;

	protected long waitTimeoutSeconds = 5;

	/**
	 * Exposes, in the web frontend, a path that the user needs to know
	 *
	 * @param name Name to use in the frontend
	 * @param path Path, relative to baseUrl
	 */
	private void exposePath(String name, String path) {
		env.putString(name, env.getString("base_url") + "/" + path);
		exposeEnvString(name);
	}

	private void exposeMtlsPath(String name, String path) {
		String baseUrlMtls = env.getString("base_mtls_url");
		env.putString(name, baseUrlMtls + "/" + path);
		exposeEnvString(name);
	}

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken(){}

	protected void addCustomValuesToAuthorizationResponse(){}

	protected void endTestIfRequiredParametersAreMissing(){}

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

		profile = getVariant(FAPI2ID2OPProfile.class);
		responseMode = getVariant(FAPIResponseMode.class);
		clientAuthType = getVariant(ClientAuthType.class);
		fapiClientType = getVariant(FAPIClientType.class);
		fapi2AuthRequestMethod = getVariant(FAPI2AuthRequestMethod.class);
		fapi2SenderConstrainMethod = getVariant(FAPI2SenderConstrainMethod.class);
		authorizationRequestType = getVariant(AuthorizationRequestType.class);

		profileRequiresMtlsEverywhere =
			profile == FAPI2ID2OPProfile.OPENBANKING_UK ||
			profile == FAPI2ID2OPProfile.CONSUMERDATARIGHT_AU ||
			profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL ||
			profile == FAPI2ID2OPProfile.CONNECTID_AU || // https://gitlab.com/idmvp/specifications/-/issues/29
			profile == FAPI2ID2OPProfile.CBUAE;

		// We create a configuration that contains mtls_endpoint_aliases in all cases - it's mandatory for clients to
		// support it as per https://datatracker.ietf.org/doc/html/rfc8705#section-5
		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		//this must come before configureResponseModeSteps due to JARM signing_algorithm dependency
		configureServerJWKS();

		call(condition(AddResponseTypeCodeToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2-1"));
		call(condition(AddIssSupportedToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2-7"));
		call(condition(AddCodeChallengeMethodToServerConfiguration.class).requirement("FAPI2-SP-ID2-5.3.1.2"));
		if (fapiClientType == FAPIClientType.OIDC) {
			call(condition(AddScopesSupportedOpenIdToServerConfiguration.class));

			if (profile != FAPI2ID2OPProfile.CONNECTID_AU) {
				call(condition(AddSubjectTypesSupportedToServerConfiguration.class).requirement("OIDCD-3"));
			}
		}


		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1");
			// FIXME - BrazilOB-5.2.3-5 seems incorrect, but no obvious replacement.
			callAndStopOnFailure(FAPIBrazilSetGrantTypesSupportedInServerConfiguration.class);
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-4");
			callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
		} else if (profile == FAPI2ID2OPProfile.CONNECTID_AU) {
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "CID-SP-5.2.2-8");
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "CID-SP-5");
			callAndStopOnFailure(AustraliaConnectIdAddClaimsSupportedToServerConfiguration.class, "CID-SP-5");
			callAndStopOnFailure(AddSubjectTypesSupportedPairwiseToServerConfiguration.class, "CID-SP-5");
		} else {
			callAndStopOnFailure(ExtractServerSigningAlg.class);
		}

		callAndStopOnFailure(AddIdTokenSigningAlgsToServerConfiguration.class);

		if (fapi2AuthRequestMethod == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION) {
			callAndStopOnFailure(FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration.class);
		}

		if (isMTLSConstrain()) {
			callAndStopOnFailure(AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration.class, "FAPI2-4.3.1-9");
		} else if (isDpopConstrain()) {
			callAndStopOnFailure(AddDpopSigningAlgValuesSupportedToServerConfiguration.class, "DPOP-5.1");
		}

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);
		call(sequence(AddPARToServerConfiguration.class));

		if(configureResponseModeSteps!=null) {
			call(sequence(configureResponseModeSteps));
		}
		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		} else {
			callAndStopOnFailure(FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		}

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			exposeMtlsPath("accounts_endpoint", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH);
			exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
			exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
			exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
		} else if (profile == FAPI2ID2OPProfile.CONNECTID_AU) {
			// nothing to expose; the endpoints all come from discovery (the userinfo endpoint is used as the resource endpoint)
		} else if (profile == FAPI2ID2OPProfile.OPENBANKING_UK) {
			exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
			exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);
		} else {
			if (isMTLSConstrain()) {
				exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
			} else {
				exposePath("accounts_endpoint", ACCOUNTS_PATH);
			}
		}

		if (authorizationRequestType == AuthorizationRequestType.RAR){
			callAndStopOnFailure(AddSupportedAuthorizationTypesToServerConfiguration.class);
		}

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");

		callAndStopOnFailure(LoadUserInfo.class);

		configureClients();

		onConfigurationCompleted();
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/**
	 * will be called at the end of configure
	 */
	protected void onConfigurationCompleted() {
		if(requireAuthorizationServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.INFO);
		}
		if(requireResourceServerEndpointDpopNonce()) {
			callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
		}
	}

	protected boolean requireAuthorizationServerEndpointDpopNonce() {
		if(isDpopConstrain()) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean requireResourceServerEndpointDpopNonce() {
		if(isDpopConstrain()) {
			return true;
		} else {
			return false;
		}
	}

	protected void configureClients()
	{
		eventLog.startBlock("Verify configuration of first client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks(false);
		validateClientConfiguration();

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
		if (profile == FAPI2ID2OPProfile.CONNECTID_AU) {
			callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class, "CID-SP-5");
		}
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

	protected void validateClientJwks(boolean isSecondClient)
	{
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI2-SP-ID2-5.4-2", "FAPI2-SP-ID2-5.4-3");
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
		// nothing to do here
		setStatus(Status.WAITING);
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


	protected Object handleClientRequestForPath(String requestId, String path){
		if (path.equals("authorize")) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return authorizationEndpoint(requestId);
		} else if (path.equals("token")) {
			if(startingShutdown){
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
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The userinfo endpoint must be called over an mTLS secured connection.");
			}
			return userinfoEndpoint(requestId);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else if (path.equals("par")) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if(profileRequiresMtlsEverywhere) {
				throw new TestFailureException(getId(), "In this ecosystem, the PAR endpoint must be called over an mTLS " +
					"secured connection using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases.");
			}
			if (clientAuthType == ClientAuthType.MTLS) {
				throw new TestFailureException(getId(), "The PAR endpoint must be called over an mTLS secured connection when using MTLS client authentication.");
			}
			return parEndpoint(requestId);
		} else if (path.equals(ACCOUNT_REQUESTS_PATH) && profile == FAPI2ID2OPProfile.OPENBANKING_UK) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return accountRequestsEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH)) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}

			if (isMTLSConstrain()) {
				throw new TestFailureException(getId(), "The accounts endpoint must be called over an mTLS secured connection.");
			}

			return accountsEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
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
		} else if (path.equals("userinfo")) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			return userinfoEndpoint(requestId);
		} else if (path.equals("par")) {
			return parEndpoint(requestId);
		}
		if (profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			if(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)) {
				return brazilHandleNewConsentRequest(requestId, false);
			} else if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
				return brazilHandleGetConsentRequest(requestId, path, false);
			}
			if(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)) {
				return brazilHandleNewConsentRequest(requestId, true);
			} else if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")) {
				return brazilHandleGetConsentRequest(requestId, path, true);
			}
			if(FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH.equals(path)) {
				return brazilHandleNewPaymentInitiationRequest(requestId);
			}
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP (using mtls) call to " + path);
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
		if(useClientCredentialsAccessToken) {
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

		if(isPayments) {
			callAndStopOnFailure(FAPIBrazilExtractCertificateSubjectFromServerJwks.class);
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedPayments.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractPaymentsConsentRequest.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, Condition.ConditionResult.FAILURE);
			//ensure aud equals endpoint url	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, Condition.ConditionResult.FAILURE, "RFC7519-4.1.3", "BrazilOB-6.1");
			//ensure ISS equals TLS certificate organizational unit
			callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1");
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			//ensure jti is uuid	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestJtiIsUUIDv4.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1");
			callAndContinueOnFailure(FAPIBrazilValidateConsentRequestIat.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

			callAndContinueOnFailure(FAPIBrazilFetchClientOrganizationJwksFromDirectory.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			env.mapKey("parsed_client_request_jwt", "new_consent_request");
			callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			env.unmapKey("parsed_client_request_jwt");

		} else {
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedConsents.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2-8");
		}

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE,"FAPI2-IMP-2.1.1");

		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if(isPayments) {
				callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentsConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2-8");
				callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1-2");
				String signedConsentResponse = env.getString("signed_consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");

				HttpHeaders headers = headersFromJson(headerJson);
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
				responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);
			} else {
				callAndContinueOnFailure(FAPIBrazilGenerateNewConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2-8");
				JsonObject response = env.getObject("consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");
				responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.CREATED);
			}
			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);
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
		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		String requestedConsentId = path.substring(path.lastIndexOf('/')+1);
		env.putString("requested_consent_id", requestedConsentId);

		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if(isPayments) {
				callAndContinueOnFailure(FAPIBrazilGenerateGetPaymentConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
				callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
				String signedConsentResponse = env.getString("signed_consent_response");
				JsonObject headerJson = env.getObject("consent_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.OK);

		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateGetConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			JsonObject response = env.getObject("consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");
			responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.OK);
		}

			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);
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
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, Condition.ConditionResult.FAILURE);

		checkResourceEndpointRequest(false);

		callAndContinueOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainPayments.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(FAPIBrazilExtractPaymentInitiationRequest.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
		env.mapKey("parsed_client_request_jwt", "payment_initiation_request");
		callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		env.unmapKey("parsed_client_request_jwt");

		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, Condition.ConditionResult.FAILURE);

		//ensure aud equals endpoint url	"BrazilOB-6.1"
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestAud.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		//ensure ISS equals TLS certificate organizational unit
		callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestIat.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");


		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			callAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			String signedConsentResponse = env.getString("signed_payment_initiation_response");
			JsonObject headerJson = env.getObject("payment_initiation_response_headers");

		HttpHeaders headers = headersFromJson(headerJson);
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

			callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);
			resourceEndpointCallComplete();
		}

		call(exec().unmapKey("incoming_request").endBlock());
		return responseEntity;
	}

	protected void resourceEndpointCallComplete() {
		// at this point we can assume the test is fully done
		fireTestFinished();
	}

	protected Object discoveryEndpoint() {
		setStatus(Status.RUNNING);
		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
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
			if(env.containsObject("incoming_dpop_proof")) {
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

		if(clientAuthType == ClientAuthType.MTLS || profileRequiresMtlsEverywhere) {
			// there is generally no requirement to present an MTLS certificate at the PAR endpoint when using private_key_jwt.
			// (This differs to the token endpoint, where an MTLS certificate must always be presented, as one is
			// required to bind the issued access token to.)
			checkMtlsCertificate();
		}

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
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

		authenticateParEndpointRequest(requestId);
		setParAuthorizationEndpointRequestParamsForHttpMethod();
		extractParEndpointRequest();

		if(env.containsObject("authorization_request_object")) {
			validateRequestObjectForPAREndpointRequest();
		}
		senderConstrainTokenRequestHelper.checkParRequest();
		if(isDpopConstrain()) {
			callAndContinueOnFailure(ExtractParAuthorizationCodeDpopBindingKey.class, ConditionResult.FAILURE, "DPOP-10");
		}

		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("par_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreatePAREndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseEntity = new ResponseEntity<>(env.getObject("par_endpoint_response"), headersFromJson(env.getObject("par_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("par_endpoint_response_http_status").intValue()));
		}  else {
			JsonObject parResponse = createPAREndpointResponse();
			responseEntity = new ResponseEntity<>(parResponse, HttpStatus.CREATED);
		}

		setStatus(Status.WAITING);
		call(exec().unmapKey("incoming_request").unmapKey("par_endpoint_http_request"));

		return responseEntity;
	}

	protected void addCustomValuesToParResponse() {}

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
		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
		}

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			if(requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}
			if (profile == FAPI2ID2OPProfile.CONNECTID_AU || profile == FAPI2ID2OPProfile.CBUAE ) {
				// for ConnectID we use the userinfo endpoint as the resource endpoint, so this is the end of the test
				resourceEndpointCallComplete();
			} else {
				setStatus(Status.WAITING);
			}
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

		if(isDpopConstrain()) {
			call(exec().mapKey("incoming_request", requestId));
		}

		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		if (clientAuthType == ClientAuthType.MTLS || isMTLSConstrain()  || profileRequiresMtlsEverywhere) {
			checkMtlsCertificate();
		}

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
			call(new ValidateClientAuthenticationWithPrivateKeyJWT().
				then(condition(ValidateClientAssertionAudClaimIsIssuerAsString.class).onFail(ConditionResult.FAILURE).requirements("FAPI2-SP-ID2-5.3.2.1-5").dontStopOnFailure())
			);
		} else {
			call(sequence(validateClientAuthenticationSteps));
		}

		Object tokenResponseOb =  handleTokenEndpointGrantType(requestId);
		if(isDpopConstrain()) {
			call(exec().unmapKey("incoming_request"));
		}
		return tokenResponseOb;

	}

	protected Object handleTokenEndpointGrantType(String requestId){

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
				if (profile == FAPI2ID2OPProfile.OPENBANKING_UK) {
					// we're doing the client credentials grant for initial token access
					return clientCredentialsGrantType(requestId);
				} else if (profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
					callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
					return clientCredentialsGrantType(requestId);
				}
				break;
			case "refresh_token":
				return refreshTokenGrantType(requestId);
		}
		throw new TestFailureException(getId(), "Got an unexpected grant type on the token endpoint: " + grantType);
	}

	protected Object refreshTokenGrantType(String requestId) {
		senderConstrainTokenRequestHelper.checkTokenRequest();
		ResponseEntity<Object> responseObject = null;

		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
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
			if(requireAuthorizationServerEndpointDpopNonce()) {
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
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {

			callAndStopOnFailure(generateSenderConstrainedAccessToken);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

			// this puts the client credentials specific token into its own box for later
			if(isMTLSConstrain()) {
				callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);
			} else  {
				callAndStopOnFailure(CopyAccessTokenToDpopClientCredentialsField.class);
			}
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);
			// Create a new DPoP nonce
			if(requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return responseObject;
	}

	protected void validateRedirectUriForAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateRedirectUri.class);
	}

	protected Object authorizationCodeGrantType(String requestId) {
		senderConstrainTokenRequestHelper.checkTokenRequest();

		ResponseEntity<Object> responseObject = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateTokenEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), headersFromJson(env.getObject("token_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("token_endpoint_response_http_status").intValue()));
		} else {
			callAndStopOnFailure(ValidateAuthorizationCode.class);

		validateRedirectUriForAuthorizationCodeGrantType();

		call(sequence(CheckPkceCodeVerifier.class));

		issueAccessToken();

		issueRefreshToken();

		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			issueIdToken(false);
		}

			createTokenEndpointResponse();
			responseObject = new ResponseEntity<>(env.getObject("token_endpoint_response"), HttpStatus.OK);

			// Create a new DPoP nonce
			if(requireAuthorizationServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateAuthorizationServerDpopNonce.class, ConditionResult.FAILURE);
			}
		}

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);
		return responseObject;
	}

	protected void createTokenEndpointResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
		if (authorizationRequestType == AuthorizationRequestType.RAR) {
			callAndStopOnFailure(RARSupport.AddRarToTokenEndpointResponse.class);
		}
	}

	protected void setAuthorizationEndpointRequestParamsForHttpMethod() {
		String httpMethod = env.getString("authorization_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("authorization_endpoint_http_request");
		if("POST".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else if("GET".equals(httpMethod)) {
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
		if("POST".equals(httpMethod)) {
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

		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationPARRequestParameters.class);

		endTestIfRequiredParametersAreMissing();
		callAndStopOnFailure(EnsureResponseTypeIsCode.class, "FAPI2-SP-ID2-5.3.1.2-1");

		skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInRequestObject.class, ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		if (profile == FAPI2ID2OPProfile.CONNECTID_AU) {
			skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
				AustraliaConnectIdCheckForFAPI2ClaimsInRequestObject.class, ConditionResult.FAILURE, "CID-IDA-5.2-2.7");
		}

		if (fapiClientType == FAPIClientType.OIDC) {
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckForUnexpectedClaimsInClaimsParameter.class, ConditionResult.WARNING, "OIDCC-5.5");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckForUnexpectedOpenIdClaims.class, ConditionResult.WARNING, "OIDCC-5.1", "OIDCC-5.5.1.1", "BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10", "OBSP-3.4");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckRequestObjectClaimsParameterValues.class, ConditionResult.FAILURE, "OIDCC-5.5");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckRequestObjectClaimsParameterMemberValues.class, ConditionResult.FAILURE, "OIDCC-5.5.1");
		}

		callAndStopOnFailure(EnsureAuthorizationRequestContainsPkceCodeChallenge.class, "FAPI2-SP-ID2-5.3.2.2-3");
		validateRequestObjectForAuthorizationEndpointRequest();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			if(fapiClientType== FAPIClientType.PLAIN_OAUTH) {
				throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
			}
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.NONCE, ConditionResult.INFO, ExtractNonceFromAuthorizationRequest.class, ConditionResult.FAILURE, "OIDCC-3.2.2.1");
		} else {
			if(fapiClientType== FAPIClientType.OIDC) {
				throw new TestFailureException(getId(), "openid scope must be used with OIDC");
			}
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE, ConditionResult.INFO, EnsureAuthorizationRequestContainsStateParameter.class, ConditionResult.FAILURE, "RFC6749-4.1.1" );
		}

		/*
			- Após o `POST` de criação do consentimento, o `STATUS` devolvido na resposta deverá ser `AWAITING_AUTHORISATION`.
			- O `STATUS` será alterado para `AUTHORISED` somente após autenticação e confirmação por parte do
				usuário na instituição transmissora dos dados.
		 */
		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
		}

		createAuthorizationEndpointResponse();

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		if(requireAuthorizationServerEndpointDpopNonce()) {
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
		callAndContinueOnFailure(FAPIValidateRequestObjectMediaType.class, Condition.ConditionResult.WARNING, "JAR-4");
		if(fapiClientType== FAPIClientType.OIDC) {
			if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
				callAndContinueOnFailure(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class, ConditionResult.FAILURE,
					"OIDCC-5.5.1.1", "BrazilOB-5.2.2-5", "BrazilOB-5.2.2-6");
			} else {
				callAndContinueOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, ConditionResult.INFO,
					"OIDCC-5.5.1.1");
			}
		}
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI2-MS-ID1-5.3.1-4");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI2-MS-ID1-5.3.1-3");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.FAILURE, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.FAILURE, "JAR-10.8");
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI2-MS-ID1-5.3.1-1");
		validateRedirectUriInRequestObject();
		if (authorizationRequestType == AuthorizationRequestType.RAR){
			callAndStopOnFailure(RARSupport.EnsureRequestObjectContainValidRAR.class, ConditionResult.FAILURE, "RAR-2.0");
		}
	}

	protected void validateRedirectUriInRequestObject() {
		callAndContinueOnFailure(EnsureMatchingRedirectUriInRequestObject.class, ConditionResult.FAILURE);
	}

	protected void validateRequestObjectForAuthorizationEndpointRequest() {
		if(fapi2AuthRequestMethod == FAPI2AuthRequestMethod.SIGNED_NON_REPUDIATION) {
			callAndContinueOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.FAILURE,
				"FAPI2-MS-ID1-5.3.2-1");
		}
		callAndStopOnFailure(ExtractRequestedScopes.class);

		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				callAndStopOnFailure(EnsureScopeContainsPayments.class);
			} else {
				callAndStopOnFailure(EnsureScopeContainsAccounts.class);
			}
		} else {
			if (profile == FAPI2ID2OPProfile.CONNECTID_AU) {
				callAndContinueOnFailure(AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims.class, ConditionResult.FAILURE, "CID-SP-5");
				callAndContinueOnFailure(AustraliaConnectIdEnsureAuthorizationRequestContainsNoAcrClaims.class, ConditionResult.FAILURE, "CID-SP-5");
				callAndContinueOnFailure(AustraliaConnectIdValidatePurpose.class, ConditionResult.FAILURE, "CID-PURPOSE-5", "CID-IDA-5.2-10");
			}
			callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScope.class);
		}

		if(fapiClientType == FAPIClientType.OIDC) {
			callAndStopOnFailure(EnsureOpenIDInScopeRequest.class);
		}

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");
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
		if(isAuthorizationEndpoint) {
			callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");
			// FIXME = No obvkous FAPI2 equivalent. PKCE replaces s_hash
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE,
				ConditionResult.INFO, CalculateSHash.class, ConditionResult.FAILURE);
		}

		callAndStopOnFailure(GenerateIdTokenClaims.class);
		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
		}

		if (!isAuthorizationEndpoint && authorizationCodeGrantTypeProfileSteps != null) {
			call(sequence(authorizationCodeGrantTypeProfileSteps));
		}

		if (isAuthorizationEndpoint && authorizationEndpointProfileSteps != null) {
			call(sequence(authorizationEndpointProfileSteps));
		}

		//TODO skip or add?
		if(isAuthorizationEndpoint) {
			callAndStopOnFailure(AddCHashToIdTokenClaims.class, "OIDCC-3.3.2.11");
			skipIfMissing(null, new String[] {"s_hash"}, ConditionResult.INFO,
				AddSHashToIdTokenClaims.class, ConditionResult.FAILURE);
		}
		skipIfMissing(null, new String[] {"at_hash"}, ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		if(profile == FAPI2ID2OPProfile.CONNECTID_AU) {
			callAndContinueOnFailure(LoadRequestedIdTokenClaims.class, ConditionResult.INFO);
		}

		addCustomValuesToIdToken();

		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				FAPIBrazilOBAddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		} else {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				AddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		}


	}
	protected void signIdToken() {
		callAndStopOnFailure(SignIdToken.class);

		addCustomSignatureOfIdToken();
	}

	/**
	 * This method does not actually encrypt id_tokens, even when id_token_encrypted_response_alg is set
	 * "5.2.3.1.  ID Token as detached signature" reads:
	 *  "5. shall support both signed and signed & encrypted ID Tokens."
	 *  So an implementation MUST support non-encrypted id_tokens too and we do NOT allow testers to run all tests with id_token
	 *  encryption enabled, encryption will be enabled only for certain tests and the rest will return non-encrypted id_tokens.
	 *  Second client will be used for encrypted id_token tests. First client does not need to have an encryption key
	 * @param isAuthorizationEndpoint
	 */
	protected void encryptIdToken(boolean isAuthorizationEndpoint) {
	}

	protected void createAuthorizationEndpointResponse() {
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		callAndStopOnFailure(AddIssToAuthorizationEndpointResponseParams.class, "FAPI2-SP-ID2-5.3.1.2-7");

		addCustomValuesToAuthorizationResponse();

		if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeQuery.class, "OIDCC-3.1.2.5");
		}
		if(responseMode==FAPIResponseMode.JARM) {
			createJARMResponse();
			//send via redirect
			callAndStopOnFailure(SendJARMResponseWitResponseModeQuery.class, "OIDCC-3.3.2.5", "JARM-2.3.1");
		}

		exposeEnvString("authorization_endpoint_response_redirect");
	}

	protected void createJARMResponse() {
		generateJARMResponseClaims();
		//authorization_signed_response_alg will not be taken into account. signing_algorithm will be used
		callAndStopOnFailure(SignJARMResponse.class,"JARM-2.2");
		encryptJARMResponse();
	}

	protected void encryptJARMResponse() {
		skipIfElementMissing("client", "authorization_encrypted_response_alg", ConditionResult.INFO,
			EncryptJARMResponse.class, ConditionResult.FAILURE, "JARM-3");

	}

	protected void addCustomValuesToJarmResponse() {}

	protected void generateJARMResponseClaims() {
		callAndStopOnFailure(GenerateJARMResponseClaims.class,"JARM-2.1.1");
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
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
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
			if(requireResourceServerEndpointDpopNonce()) {
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

		if(profile == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				throw new TestFailureException(getId(), FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + " was called. The test must end at the payment initiation endpoint");
			}
		}

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI2-IMP-2.1.1");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		if (accountsEndpointProfileSteps != null) {
			call(sequence(accountsEndpointProfileSteps));
		}

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> responseEntity = null;
		if(isDpopConstrain() && !Strings.isNullOrEmpty(env.getString("resource_endpoint_dpop_nonce_error"))) {
			callAndContinueOnFailure(CreateResourceEndpointDpopErrorResponse.class, ConditionResult.FAILURE);
			setStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(env.getObject("resource_endpoint_response"), headersFromJson(env.getObject("resource_endpoint_response_headers")), HttpStatus.valueOf(env.getInteger("resource_endpoint_response_http_status").intValue()));
		} else {
			JsonObject accountsEndpointResponse = env.getObject("accounts_endpoint_response");
			JsonObject headerJson = env.getObject("accounts_endpoint_response_headers");

			if(requireResourceServerEndpointDpopNonce()) {
				callAndContinueOnFailure(CreateResourceServerDpopNonce.class, ConditionResult.INFO);
			}
			// at this point we can assume the test is fully done
			resourceEndpointCallComplete();
			responseEntity = new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
		}
		return responseEntity;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointAuthMethodSupported = AddTLSClientAuthToServerConfiguration.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		authorizationCodeGrantTypeProfileSteps = AddOpenBankingUkClaimsToAuthorizationCodeGrant.class;
		authorizationEndpointProfileSteps = AddOpenBankingUkClaimsToAuthorizationEndpointResponse.class;
		accountsEndpointProfileSteps = GenerateOpenBankingUkAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = GenerateOpenBankingBrazilAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPI2ID2OPProfile.class, value = "connectid_au")
	public void setupConnectIdAu() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "plain_response")
	public void setupResponseModePlain() {
		configureResponseModeSteps = null;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "jarm")
	public void setupResponseModeJARM() {
		configureResponseModeSteps = AddJARMToServerConfiguration.class;
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
