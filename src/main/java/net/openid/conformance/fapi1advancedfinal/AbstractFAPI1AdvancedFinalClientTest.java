package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AbstractBrazilAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeChallengeMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenSigningAlgsToServerConfiguration;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddSHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddSubjectTypesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CalculateSHash;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CheckForFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInClaimsParameter;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInRequestObject;
import net.openid.conformance.condition.as.CheckForUnexpectedOpenIdClaims;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterMemberValues;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterValues;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.EncryptJARMResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsStateParameter;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureIdTokenEncryptedResponseAlgIsNotRSA1_5;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureMatchingRedirectUriInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsurePAREndpointRequestDoesNotContainRequestUriParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequestObjectWasEncrypted;
import net.openid.conformance.condition.as.EnsureRequestedScopeIsEqualToConfiguredScope;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeIdToken;
import net.openid.conformance.condition.as.EnsureScopeContainsAccounts;
import net.openid.conformance.condition.as.EnsureScopeContainsCustomers;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddOBBrazilAcrValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddOPINBrazilAcrValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.as.FAPIBrazilExtractConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentInitiationRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentsConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilOPINAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetGrantTypesSupportedInServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentConsentResponse;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentInitiationResponse;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIKSAValidateConsentScope;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectExp;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectMediaType;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateAccessTokenExpiration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateServerConfigurationMTLS;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeFragment;
import net.openid.conformance.condition.as.SetParEndpointToMtlsParEndpoint;
import net.openid.conformance.condition.as.SetRequestParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.SetRsaAltServerJwks;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateClientAssertionAudClaimForPAREndpoint;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientAssertionClaimsForPAREndpoint;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.as.ValidateRedirectUri;
import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.jarm.GenerateJARMResponseClaims;
import net.openid.conformance.condition.as.jarm.SendJARMResponseWitResponseModeQuery;
import net.openid.conformance.condition.as.jarm.SignJARMResponse;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR;
import net.openid.conformance.condition.as.par.ExtractRequestObjectFromPAREndpointRequest;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse;
import net.openid.conformance.condition.rs.CreateFAPIResourcesEndpointResponse;
import net.openid.conformance.condition.rs.CreateKSAOBAccountRequestResponse;
import net.openid.conformance.condition.rs.CreateOpenBankingAccountRequestResponse;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractFapiDateHeader;
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader;
import net.openid.conformance.condition.rs.ExtractXIdempotencyKeyHeader;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainCustomers;
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
import net.openid.conformance.condition.rs.GenerateKSAAccountConsentId;
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.condition.rs.RequireBearerClientCredentialsAccessToken;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddJARMToServerConfiguration;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationCodeGrant;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationEndpointResponse;
import net.openid.conformance.sequence.as.AddPARToServerConfiguration;
import net.openid.conformance.sequence.as.AddPlainFAPIToServerConfiguration;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.sequence.as.GenerateOpenBankingUkAccountsEndpointResponse;
import net.openid.conformance.sequence.as.GenerateOpenInsuranceBrazilResourcesEndpointResponse;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
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

import java.util.Arrays;
import java.util.List;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	FAPIAuthRequestMethod.class,
	FAPIResponseMode.class,
	FAPIClientType.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.keystore"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil", configurationFields = {
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.scope",
	"client2.scope"
})
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil", configurationFields = {
	"client.scope",
	"client2.scope"
})
public abstract class AbstractFAPI1AdvancedFinalClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";
	public static final String RESOURCES_PATH = "resources/v1";

	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	private Class<? extends ConditionSequence> configureAuthRequestMethodSteps;
	private Class<? extends ConditionSequence> configureResponseModeSteps;
	private Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	private Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;
	private Class<? extends ConditionSequence> resourcesEndpointProfileSteps;

	// Controls which endpoints we should expose to the client
	protected FAPI1FinalOPProfile profile;

	protected FAPIAuthRequestMethod authRequestMethod;

	protected FAPIResponseMode responseMode;

	protected ClientAuthType clientAuthType;

	protected FAPIClientType fapiClientType;

	protected boolean startingShutdown = false;

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

	protected void addCustomAudToPaymentsConsentResponse(){}

	protected void addCustomSignatureOfIdToken(){}

	protected void endTestIfRequiredParametersAreMissing(){}

	protected boolean isBrazil() {
		return profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL ||
			profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL;
	}

	protected boolean isKSA() {
		return profile == FAPI1FinalOPProfile.OPENBANKING_KSA;
	}

	protected boolean isOpenInsurance() {
		return profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		profile = getVariant(FAPI1FinalOPProfile.class);
		authRequestMethod = getVariant(FAPIAuthRequestMethod.class);
		responseMode = getVariant(FAPIResponseMode.class);
		clientAuthType = getVariant(ClientAuthType.class);
		fapiClientType = getVariant(FAPIClientType.class);

		// We create a configuration that contains mtls_endpoint_aliases in all cases - it's mandatory for clients to
		// support it as per https://datatracker.ietf.org/doc/html/rfc8705#section-5
		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		if(authRequestMethod == FAPIAuthRequestMethod.BY_VALUE) {
			callAndStopOnFailure(SetRequestParameterSupportedToTrueInServerConfiguration.class);
		} else {
			call(condition(AddCodeChallengeMethodToServerConfiguration.class).requirement("FAPI1-ADV-5.2.2-18"));
		}

		if (fapiClientType == FAPIClientType.OIDC) {
			call(condition(AddSubjectTypesSupportedToServerConfiguration.class).requirement("OIDCD-3"));
		}

		//this must come before configureResponseModeSteps due to JARM signing_algorithm dependency
		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(SetRsaAltServerJwks.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		if(isBrazil()) {
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
			callAndStopOnFailure(FAPIBrazilSetGrantTypesSupportedInServerConfiguration.class, "BrazilOB-5.2.3-5");
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
			callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
			if (profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL) {
				callAndStopOnFailure(FAPIBrazilAddOPINBrazilAcrValuesSupportedToServerConfiguration.class, "BrazilOB-5.2.2");
			} else {
				callAndStopOnFailure(FAPIBrazilAddOBBrazilAcrValuesSupportedToServerConfiguration.class, "BrazilOB-5.2.2");
			}
		} else {
			callAndStopOnFailure(ExtractServerSigningAlg.class);
		}

		callAndStopOnFailure(AddIdTokenSigningAlgsToServerConfiguration.class);
		callAndStopOnFailure(AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration.class, "FAPI2-4.3.1-9");

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);

		if(configureAuthRequestMethodSteps!=null) {
			call(sequence(configureAuthRequestMethodSteps));
		}

		if (isBrazil() && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			// Brazil require the use of MTLS everywhere, so the MTLS version of the PAR endpoint must be published at the root as per
			// https://gitlab.com/openid/conformance-suite/-/issues/1041
			callAndStopOnFailure(SetParEndpointToMtlsParEndpoint.class);
		}

		if(configureResponseModeSteps!=null) {
			call(sequence(configureResponseModeSteps));
		}
		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		} else {
			callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		}

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		switch (profile) {
			case OPENBANKING_BRAZIL:
				exposeMtlsPath("accounts_endpoint", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH);
				exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
				exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
				exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
				break;
			case OPENINSURANCE_BRAZIL:
				exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_OPIN_CONSENTS_PATH);
				exposeMtlsPath("resources_endpoint", FAPIBrazilRsPathConstants.BRAZIL_OPIN_CUSTOMERS_PATH);
				break;
			case OPENBANKING_UK:
				exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
				exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);
				break;
			case OPENBANKING_KSA:
				exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
				exposeMtlsPath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);
				break;
			default:
				exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
				break;
		}

		if(authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			exposeMtlsPath("par_endpoint", "par");
		}

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI1-BASE-5.2.2-5", "FAPI1-BASE-5.2.2-6");

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
		if (isBrazil()) {
			switchToSecondClient();
		}
	}

	protected void configureClients() {
		eventLog.startBlock("Verify configuration of first client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks(false);
		validateClientConfiguration();

		if (isBrazil()) {
			// we always use the second client in Brazil, as that's the one that has configuration
			// for id_token encryption which is mandatory in the Brazil v2 security profile
			configureSecondClient();
		}

	}

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

	protected void validateClientJwks(boolean isSecondClient)
	{
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI1-BASE-5.2.4-2", "FAPI1-BASE-5.2.4-3");

		if(isSecondClient) {
			//ensure that there is a key we can use for id_token encryption
			callAndStopOnFailure(EnsureIdTokenEncryptedResponseAlgIsNotRSA1_5.class, "FAPI1-ADV-8.6.1-1");
			callAndStopOnFailure(FAPIEnsureClientJwksContainsAnEncryptionKey.class, "FAPI1-ADV-5.2.3.1-5", "FAPI1-ADV-8.6.1-1");
			callAndStopOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, "OIDCR-2", "FAPI1-ADV-5.2.2.1-6");
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		// nothing to do here
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		// Allow calls to jkws and discovery even after the test is finished,
		List<String> pathsThatMayBeCalledAfterFinish = Arrays.asList("jwks", ".well-known/openid-configuration");
		if (Status.FINISHED.equals(getStatus()) && pathsThatMayBeCalledAfterFinish.contains(path)) {
			return handleClientRequestForPath(null, path);
		}

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

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
			if(isBrazil() || isKSA()) {
				throw new TestFailureException(getId(), "Token endpoint must be called over an mTLS secured connection " +
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
			if(isKSA()) {
				throw new TestFailureException(getId(), "User info endpoint must be called over an mTLS secured connection " +
						"using the userinfo_endpoint found in mtls_endpoint_aliases.");
			} else {
				return userinfoEndpoint(requestId);
			}
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if(isBrazil() || isKSA()) {
				throw new TestFailureException(getId(), "In Brazil, the PAR endpoint must be called over an mTLS " +
					"secured connection using the pushed_authorization_request_endpoint found in mtls_endpoint_aliases.");
			}
			if (clientAuthType == ClientAuthType.MTLS) {
				throw new TestFailureException(getId(), "The PAR endpoint must be called over an mTLS secured connection.");
			}
			return parEndpoint(requestId);
		} else if (path.equals(ACCOUNT_REQUESTS_PATH) && profile == FAPI1FinalOPProfile.OPENBANKING_UK) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if(isKSA()) {
				throw new TestFailureException(getId(), "Token endpoint must be called over an mTLS secured connection " +
						"using the token_endpoint found in mtls_endpoint_aliases.");
			} else {
				return accountRequestsEndpoint(requestId);
			}
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5-1");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH) || path.equals(FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH)) {
			return accountsEndpoint(requestId);
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			return parEndpoint(requestId);
		}
		if (isBrazil()) {

			String consentPath = profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL
				? FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH
				: FAPIBrazilRsPathConstants.BRAZIL_OPIN_CONSENTS_PATH;
			if(consentPath.equals(path)) {
				return brazilHandleNewConsentRequest(requestId, false);
			} else if(path.startsWith(consentPath + "/")) {
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


			if(RESOURCES_PATH.equals(path)) {
				return resourcesEndpoint(requestId);
			} else if (FAPIBrazilRsPathConstants.BRAZIL_OPIN_CUSTOMERS_PATH.equals(path)) {
				return resourcesEndpoint(requestId);
			}
		}
		if (isKSA() && path.equals(ACCOUNT_REQUESTS_PATH)){
			return ksaAccountRequestEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP (using mtls) call to " + path);
	}

	protected void validateResourceEndpointHeaders() {
		skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-3");

		skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address", ConditionResult.INFO,
			ExtractFapiIpAddressHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-4");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");
		callAndContinueOnFailure(ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");
	}
	protected void checkResourceEndpointRequest(boolean useClientCredentialsAccessToken) {
		callAndContinueOnFailure(EnsureBearerAccessTokenNotInParams.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-1");
		callAndContinueOnFailure(ExtractBearerAccessTokenFromHeader.class, Condition.ConditionResult.FAILURE,  "FAPI1-BASE-6.2.2-1");
		if(useClientCredentialsAccessToken) {
			callAndContinueOnFailure(RequireBearerClientCredentialsAccessToken.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndContinueOnFailure(RequireBearerAccessToken.class, Condition.ConditionResult.FAILURE);
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

		if (!isOpenInsurance()) {
			callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE);
		}

		if(isPayments) {
			callAndStopOnFailure(FAPIBrazilExtractCertificateSubjectFromServerJwks.class);
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedPayments.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractPaymentsConsentRequest.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
			callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-4");
			callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, Condition.ConditionResult.FAILURE);
			//ensure aud equals endpoint url	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, Condition.ConditionResult.FAILURE,"RFC7519-4.1.3", "BrazilOB-6.1-3");
			//ensure ISS equals TLS certificate organizational unit
			callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
			//ensure jti is uuid	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestJtiIsUUIDv4.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilValidateConsentRequestIat.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");

			callAndContinueOnFailure(FAPIBrazilFetchClientOrganizationJwksFromDirectory.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-6");
			env.mapKey("parsed_client_request_jwt", "new_consent_request");
			callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-6");
			env.unmapKey("parsed_client_request_jwt");

		} else {
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedConsents.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
		}

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE,"FAPI1-BASE-6.2.1-11");

		ResponseEntity<Object> responseEntity = null;
		if(isPayments) {
			callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentsConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
			addCustomAudToPaymentsConsentResponse();
			callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-6.1-2");
			String signedConsentResponse = env.getString("signed_consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);
		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateNewConsentResponse.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
			JsonObject response = env.getObject("consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");
			responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.CREATED);
		}
		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);

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

		if (!isOpenInsurance()) {
			callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE);
		}

		checkResourceEndpointRequest(true);
		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		String requestedConsentId = path.substring(path.lastIndexOf('/')+1);
		env.putString("requested_consent_id", requestedConsentId);

		ResponseEntity<Object> responseEntity = null;
		if(isPayments) {
			callAndContinueOnFailure(FAPIBrazilGenerateGetPaymentConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-2");
			String signedConsentResponse = env.getString("signed_consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);

			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.OK);

		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateGetConsentResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
			JsonObject response = env.getObject("consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");
			responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.OK);
		}

		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);

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

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainPayments.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(FAPIBrazilExtractPaymentInitiationRequest.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
		env.mapKey("parsed_client_request_jwt", "payment_initiation_request");
		callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-6");
		env.unmapKey("parsed_client_request_jwt");

		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-4");

		callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, Condition.ConditionResult.FAILURE);

		//ensure aud equals endpoint url	"BrazilOB-6.1"
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestAud.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
		//ensure ISS equals TLS certificate organizational unit
		callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestIat.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-3");

		ResponseEntity<Object> responseEntity;
		callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
		callAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1-2");
		String signedConsentResponse = env.getString("signed_payment_initiation_response");
		JsonObject headerJson = env.getObject("payment_initiation_response_headers");

		HttpHeaders headers = headersFromJson(headerJson);
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, Condition.ConditionResult.FAILURE);

		call(exec().unmapKey("incoming_request").endBlock());
		resourceEndpointCallComplete();

		return responseEntity;
	}

	protected void resourceEndpointCallComplete() {
		// at this point we can assume the test is fully done
		fireTestFinished();
	}

	protected Object discoveryEndpoint() {
		setStatusUnlessTestIsFinished(Status.RUNNING);
		JsonObject serverConfiguration = env.getObject("server");

		setStatusUnlessTestIsFinished(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	protected void checkMtlsCertificate() {
		callAndContinueOnFailure(ExtractClientCertificateFromRequestHeaders.class, ConditionResult.FAILURE);
		callAndStopOnFailure(CheckForClientCertificate.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-5");
		callAndContinueOnFailure(EnsureClientCertificateMatches.class, ConditionResult.FAILURE);
	}
	protected void authenticateParEndpointRequest(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));

		if(clientAuthType == ClientAuthType.MTLS || isBrazil()) {
			// there is no requirement to present an MTLS certificate at the PAR endpoint when using private_key_jwt.
			// (This differs to the token endpoint, where an MTLS certificate must always be presented, as one is
			// required to bind the issued access token to.)
			// The exception is Brazil, where a TLS client certificate must be presented to all endpoints in all cases.
			checkMtlsCertificate();
		}

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
			call(new ValidateClientAuthenticationWithPrivateKeyJWT().
				replace(ValidateClientAssertionClaims.class, condition(ValidateClientAssertionClaimsForPAREndpoint.class).requirements("PAR-2")).then(condition(ValidateClientAssertionAudClaimForPAREndpoint.class).onFail(ConditionResult.WARNING).requirements("PAR-2").dontStopOnFailure())
			);
		} else {
			call(sequence(validateClientAuthenticationSteps));
		}
		call(exec().unmapKey("token_endpoint_request"));
	}

	protected void extractParEndpointRequest() {
		callAndStopOnFailure(ExtractRequestObjectFromPAREndpointRequest.class, "PAR-2.1");
		callAndStopOnFailure(EnsurePAREndpointRequestDoesNotContainRequestUriParameter.class, "PAR-2.1");
		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
	}

	protected Object parEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("PAR endpoint").mapKey("par_endpoint_http_request", requestId));

		authenticateParEndpointRequest(requestId);
		extractParEndpointRequest();
		validateRequestObjectForPAREndpointRequest();

		JsonObject parResponse = createPAREndpointResponse();
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(parResponse, HttpStatus.CREATED);
	}

	protected JsonObject createPAREndpointResponse() {
		callAndStopOnFailure(CreatePAREndpointResponse.class, "PAR-2.2");
		JsonObject parResponse = env.getObject("par_endpoint_response");
		return parResponse;
	}

	protected Object userinfoEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint")
			.mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(FilterUserInfoForScopes.class);
		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
		}

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	protected Object jwksEndpoint() {
		setStatusUnlessTestIsFinished(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatusUnlessTestIsFinished(Status.WAITING);
		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object tokenEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint")
			.mapKey("token_endpoint_request", requestId));

		if (env.getObject("client") == null) {
			throw new TestFailureException(getId(), "The token endpoint has been called before the client has been registered.");
		}

		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		checkMtlsCertificate();

		call(sequence(validateClientAuthenticationSteps));

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw new TestFailureException(getId(), "Token endpoint body does not contain the mandatory 'grant_type' parameter" + grantType);
		}

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else if (grantType.equals("client_credentials")) {
			if( profile == FAPI1FinalOPProfile.OPENBANKING_UK || isKSA()) {
				// we're doing the client credentials grant for initial token access
				return clientCredentialsGrantType(requestId);
			} else if(isBrazil()) {
				callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
				return clientCredentialsGrantType(requestId);
			}
		} else if (grantType.equals("refresh_token")) {
			return refreshTokenGrantType(requestId);
		}
		throw new TestFailureException(getId(), "Got an unexpected grant type on the token endpoint: " + grantType);
	}

	protected Object refreshTokenGrantType(String requestId) {

		callAndStopOnFailure(ValidateRefreshToken.class);

		issueAccessToken();
		issueRefreshToken(); // rotate refresh token
		env.removeNativeValue("id_token");
		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected Object clientCredentialsGrantType(String requestId) {

		callAndStopOnFailure(GenerateBearerAccessToken.class);
		callAndContinueOnFailure(GenerateAccessTokenExpiration.class, ConditionResult.INFO);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		// this puts the client credentials specific token into its own box for later
		callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected void setStatusUnlessTestIsFinished(Status status) {
		if (!Status.FINISHED.equals(getStatus())) {
			setStatus(status);
		}
	}

	protected void validateRedirectUriForAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateRedirectUri.class);
	}

	protected Object authorizationCodeGrantType(String requestId) {

		callAndStopOnFailure(ValidateAuthorizationCode.class);

		validateRedirectUriForAuthorizationCodeGrantType();

		if(authRequestMethod==FAPIAuthRequestMethod.PUSHED) {
			call(sequence(CheckPkceCodeVerifier.class));
		}

		issueAccessToken();

		issueRefreshToken();

		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			issueIdToken(false);
		}

		createTokenEndpointResponse();

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected void createTokenEndpointResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
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

	@UserFacing
	protected Object authorizationEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();
		if(authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			callAndStopOnFailure(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR.class);
			callAndContinueOnFailure(EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR.class, Condition.ConditionResult.WARNING);
		}

		if(authRequestMethod == FAPIAuthRequestMethod.BY_VALUE) {
			callAndStopOnFailure(ExtractRequestObject.class, "FAPI1-ADV-5.2.2-10");
			if(isBrazil()) {
				callAndStopOnFailure(EnsureRequestObjectWasEncrypted.class, "BrazilOB-5.2.3-3");
				callAndStopOnFailure(FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM.class, "BrazilOB-6.1.1-1");
			}
		}

		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInRequestObject.class, ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		if (fapiClientType == FAPIClientType.OIDC) {
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckForUnexpectedClaimsInClaimsParameter.class, ConditionResult.WARNING, "OIDCC-5.5");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckForUnexpectedOpenIdClaims.class, ConditionResult.WARNING, "OIDCC-5.1", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.3", "BrazilOB-5.2.2.4", "OBSP-3.4");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckRequestObjectClaimsParameterValues.class, ConditionResult.FAILURE, "OIDCC-5.5");
			skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
				CheckRequestObjectClaimsParameterMemberValues.class, ConditionResult.FAILURE, "OIDCC-5.5.1");
		}

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class);

		endTestIfRequiredParametersAreMissing();

		validateRequestObjectForAuthorizationEndpointRequest();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			if(fapiClientType== FAPIClientType.PLAIN_OAUTH) {
				throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
			}
			callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, "FAPI1-BASE-5.2.2.2");
		} else {
			if(fapiClientType== FAPIClientType.OIDC) {
				throw new TestFailureException(getId(), "openid scope must be used with OIDC");
			}
			callAndStopOnFailure(EnsureAuthorizationRequestContainsStateParameter.class, "FAPI1-BASE-5.2.2.3-1");
		}

		if(responseMode!=FAPIResponseMode.JARM && "yes".equals(isOpenIdScopeRequested)) {
			//issueAccessToken();
			issueIdToken(true);
		}

		/*
		 	- Após o `POST` de criação do consentimento, o `STATUS` devolvido na resposta deverá ser `AWAITING_AUTHORISATION`.
			- O `STATUS` será alterado para `AUTHORISED` somente após autenticação e confirmação por parte do
				usuário na instituição transmissora dos dados.
		 */
		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
		}

		createAuthorizationEndpointResponse();

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return new RedirectView(redirectTo, false, false, false);

	}

	/**
	 * Common checks applicable to both PAR endpoint and authorization requests
	 */
	protected void validateRequestObjectCommonChecks() {
		callAndStopOnFailure(FAPIValidateRequestObjectSigningAlg.class, "FAPI1-ADV-8.6");
		callAndContinueOnFailure(FAPIValidateRequestObjectMediaType.class, Condition.ConditionResult.WARNING, "JAR-4");
		if(fapiClientType== FAPIClientType.OIDC) {
			if(isBrazil()) {
				callAndContinueOnFailure(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class, ConditionResult.FAILURE,
					"OIDCC-5.5.1.1", "BrazilOB-5.2.3-5", "BrazilOB-5.2.3-6");
			} else {
				callAndContinueOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, ConditionResult.INFO,
					 "OIDCC-5.5.1.1");
			}
		}
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI1-ADV-5.2.2-13");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-17");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.FAILURE, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.FAILURE, "JAR-10.8");
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI1-ADV-5.2.2-1");
		validateRedirectUriInRequestObject();
	}

	protected void validateRedirectUriInRequestObject() {
		callAndContinueOnFailure(EnsureMatchingRedirectUriInRequestObject.class, ConditionResult.FAILURE);
	}

	protected void validateRequestObjectForAuthorizationEndpointRequest() {
		if(authRequestMethod==FAPIAuthRequestMethod.PUSHED) {
			callAndContinueOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.FAILURE,
				"FAPI1-ADV-5.2.3-16");
		} else {
			validateRequestObjectCommonChecks();	//for PAR, these checks will be applied to the PAR endpoint request
			callAndContinueOnFailure(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.class,  ConditionResult.FAILURE,
				"OIDCC-6.1", "FAPI1-ADV-5.2.3-9");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.WARNING,
				"OIDCC-6.1", "OIDCC-6.2");
			if(responseMode!=FAPIResponseMode.JARM) {
				callAndContinueOnFailure(EnsureAuthorizationHttpRequestContainsOpenIDScope.class, ConditionResult.FAILURE,
					"OIDCC-6.1", "OIDCC-6.2");
			}
		}
		callAndStopOnFailure(ExtractRequestedScopes.class);

		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				callAndStopOnFailure(EnsureScopeContainsPayments.class);
			} else {
				if (profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL) {
					callAndStopOnFailure(EnsureScopeContainsCustomers.class);
				} else {
					callAndStopOnFailure(EnsureScopeContainsAccounts.class);
				}
			}
		} else if (isKSA()){
			callAndStopOnFailure(FAPIKSAValidateConsentScope.class);
		} else {
			callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScope.class);
		}

		if(responseMode==FAPIResponseMode.JARM) {
			callAndStopOnFailure(EnsureResponseTypeIsCode.class, "FAPI1-ADV-5.2.2-2");
		} else if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(EnsureResponseTypeIsCodeIdToken.class, "OIDCC-6.1", "FAPI1-ADV-5.2.2-1");
			callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI1-BASE-5.2.3-7");
		}

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");
	}

	protected void validateRequestObjectForPAREndpointRequest() {
		validateRequestObjectCommonChecks();
		callAndStopOnFailure(EnsureRequestObjectContainsCodeChallengeWhenUsingPAR.class, "FAPI1-ADV-5.2.3-15");
	}

	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		prepareIdTokenClaims(isAuthorizationEndpoint);

		signIdToken();

		encryptIdToken(isAuthorizationEndpoint);
	}

	protected void issueAccessToken() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
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
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE,
				ConditionResult.INFO, CalculateSHash.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
		}

		callAndStopOnFailure(GenerateIdTokenClaims.class);
		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
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
				AddSHashToIdTokenClaims.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
		}
		skipIfMissing(null, new String[] {"at_hash"}, ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		if(isBrazil()) {
			Class<? extends AbstractBrazilAddACRClaimToIdTokenClaims> addAcrClaimClass =
				FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL.equals(profile)
				? FAPIBrazilOPINAddACRClaimToIdTokenClaims.class
				: FAPIBrazilOBAddACRClaimToIdTokenClaims.class;
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				addAcrClaimClass, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
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
		if (isBrazil()) {
			// encryption is always required in new Brazil security profile
			callAndStopOnFailure(EncryptIdToken.class, "OIDCC-10.2", "FAPI1-ADV-5.2.2.1-6");
		}
	}

	protected void createAuthorizationEndpointResponse() {
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

		if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeFragment.class, "OIDCC-3.3.2.5");
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

	protected void generateJARMResponseClaims() {
		callAndStopOnFailure(GenerateJARMResponseClaims.class,"JARM-2.1.1");
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

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");

		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		// TODO: should we clear the old headers?
		validateResourceEndpointHeaders();

		callAndStopOnFailure(GenerateAccountRequestId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(CreateOpenBankingAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	protected Object accountsEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Accounts endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		checkMtlsCertificate();

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		checkResourceEndpointRequest(false);

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				throw new TestFailureException(getId(), FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + " was called. The test must end at the payment initiation endpoint");
			}
		}

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		if (accountsEndpointProfileSteps != null) {
			call(sequence(accountsEndpointProfileSteps));
		}

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		JsonObject accountsEndpointResponse = env.getObject("accounts_endpoint_response");
		JsonObject headerJson = env.getObject("accounts_endpoint_response_headers");

		// at this point we can assume the test is fully done
		resourceEndpointCallComplete();

		return new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	protected Object resourcesEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Resources endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		if (!isOpenInsurance()) {
			callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE);
		}

		call(exec().mapKey("incoming_request", requestId));
		checkResourceEndpointRequest(false);
		callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainCustomers.class);
		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");
		callAndStopOnFailure(CreateFAPIResourcesEndpointResponse.class);
		if (resourcesEndpointProfileSteps != null) {
			call(sequence(resourcesEndpointProfileSteps));
		}
		callAndStopOnFailure(ClearAccessTokenFromRequest.class);
		call(exec().unmapKey("incoming_request").endBlock());

		JsonObject endpointResponse = env.getObject("resources_endpoint_response");
		JsonObject headerJson = env.getObject("resources_endpoint_response_headers");

		resourceEndpointCallComplete();
		return new ResponseEntity<>(endpointResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	/**
	 * KSA OpenBanking Request consent to access accounts
	 */
	protected Object ksaAccountRequestEndpoint(String requestId){
		setStatus(Status.RUNNING);
		call(exec().startBlock("New consent endpoint").mapKey("incoming_request", requestId));

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		//Requires method=POST. defined in API docs
		callAndStopOnFailure(EnsureIncomingRequestMethodIsPost.class);

		checkResourceEndpointRequest(true);
		//callAndContinueOnFailure(FAPIKSAEnsureAuthorizationRequestScopesContainAccounts.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE,"FAPI1-BASE-6.2.1-11");

		//TODO: validate the consent structure
		//callAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, Condition.ConditionResult.FAILURE,"BrazilOB-5.2.2.2");

		callAndStopOnFailure(GenerateKSAAccountConsentId.class);
		exposeEnvString("account_request_id");

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, Condition.ConditionResult.FAILURE,"FAPI1-BASE-6.2.1-11");
		callAndStopOnFailure(CreateKSAOBAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);

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

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		authorizationCodeGrantTypeProfileSteps = AddOpenBankingUkClaimsToAuthorizationCodeGrant.class;
		authorizationEndpointProfileSteps = AddOpenBankingUkClaimsToAuthorizationEndpointResponse.class;
		accountsEndpointProfileSteps = GenerateOpenBankingUkAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_ksa")
	public void setupOpenBankingKSA() {
		//authorizationCodeGrantTypeProfileSteps = null;
		//authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = GenerateOpenBankingUkAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		//authorizationCodeGrantTypeProfileSteps = null;
		//authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = GenerateOpenBankingBrazilAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil")
	public void setupOpenInsuranceBrazil() {
		//authorizationCodeGrantTypeProfileSteps = null;
		//authorizationEndpointProfileSteps = null;
		resourcesEndpointProfileSteps = GenerateOpenInsuranceBrazilResourcesEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPIAuthRequestMethod.class, value = "by_value")
	public void setupAuthRequestMethodByValue() {
		configureAuthRequestMethodSteps = null;
	}

	@VariantSetup(parameter = FAPIAuthRequestMethod.class, value = "pushed")
	public void setupAuthRequestMethodPushed() {
		configureAuthRequestMethodSteps = AddPARToServerConfiguration.class;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "plain_response")
	public void setupResponseModePlain() {
		configureResponseModeSteps = AddPlainFAPIToServerConfiguration.class;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "jarm")
	public void setupResponseModeJARM() {
		configureResponseModeSteps = AddJARMToServerConfiguration.class;
	}

	protected void startWaitingForTimeout() {
		this.startingShutdown = true;
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the client hasn't called the token endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";

		});
	}
}
