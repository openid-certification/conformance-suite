package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddIdTokenSigningAlgsToServerConfiguration;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CheckCIBAModeIsPoll;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureScopeContainsAccounts;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
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
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentConsentResponse;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentInitiationResponse;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectExp;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectMediaType;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateIdTokenClaimsWith181DayExp;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SetRsaAltServerJwks;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.SignIdTokenWithX5tS256;
import net.openid.conformance.condition.as.ValidateFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePingToTokenDeliveryModesSupported;
import net.openid.conformance.condition.client.AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenExcludingIat;
import net.openid.conformance.condition.client.ValidateIdTokenHasRequiredBrazilHeaders;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.client.VerifyIdTokenValidityIsMinimum180Days;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
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
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.condition.rs.RequireBearerClientCredentialsAccessToken;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	CIBAMode.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {
	"openbanking_uk", "consumerdataright_au", "openbanking_ksa"
})
@VariantNotApplicable(parameter = CIBAMode.class, values = {
	"push"
})
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.scope"
})
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil", configurationFields = {
		"client.scope",
		"directory.keystore"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.keystore"
})
@VariantHidesConfigurationFields(parameter = CIBAMode.class, value = "poll", configurationFields = {
	"client.backchannel_client_notification_endpoint"
})
public abstract class AbstractFAPICIBAClientTest extends AbstractTestModule {

	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";

	protected FAPI1FinalOPProfile profile;
	protected ClientAuthType clientAuthType;
	protected CIBAMode cibaMode;

	protected boolean startingShutdown = false;

	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateTokenEndpointClientAuthenticationSteps;
	private Class<? extends ConditionSequence> validateBackchannelClientAuthenticationSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;
	private Class<? extends Condition> profileSpecificSignIdToken;

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointAuthMethodSupported = AddTLSClientAuthToServerConfiguration.class;
		validateTokenEndpointClientAuthenticationSteps = ValidateClientAuthenticationWithMTLS.class;
		validateBackchannelClientAuthenticationSteps = BackchannelValidateClientAuthenticationWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateTokenEndpointClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
		validateBackchannelClientAuthenticationSteps = BackchannelValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		profileSpecificSignIdToken = SignIdToken.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		accountsEndpointProfileSteps = GenerateOpenBankingBrazilAccountsEndpointResponse.class;
		profileSpecificSignIdToken = SignIdTokenWithX5tS256.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openinsurance_brazil")
	public void setupOpenInsuranceBrazil() {
		// we might want to generate an open insurance specific response at some point
		accountsEndpointProfileSteps = GenerateOpenBankingBrazilAccountsEndpointResponse.class;
		profileSpecificSignIdToken = SignIdTokenWithX5tS256.class;
	}

	protected boolean isBrazil() {
		return profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL ||
			profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL;
	}

	protected void addCustomValuesToIdToken() {	}

	protected void addCustomSignatureOfIdToken() { }

	protected void onConfigurationCompleted() { }

	protected void validateClientConfiguration() { }

	protected void backchannelEndpointCallComplete() {
		setStatus(Status.WAITING);
	}

	protected void tokenEndpointCallComplete() {
		callAndStopOnFailure(SetNextAllowedTokenRequest.class);
		setStatus(Status.WAITING);
	}

	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
		return HttpStatus.OK;
	}

	protected void createIntermediateTokenResponse() {
		callAndStopOnFailure(CreateAuthorizationPendingResponse.class);
	}

	protected void createFinalTokenResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
	}

	protected void sendPingRequestAndVerifyResponse() {
		callAndStopOnFailure(PingClientNotificationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA");
		callAndStopOnFailure(VerifyPingHttpResponseStatusCodeIsNot3XX.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");
		callAndContinueOnFailure(VerifyPingHttpResponseStatusCodeIs204.class, Condition.ConditionResult.WARNING, "CIBA-10.2");
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		profile = getVariant(FAPI1FinalOPProfile.class);
		clientAuthType = getVariant(ClientAuthType.class);
		cibaMode = getVariant(CIBAMode.class);
		env.putString("ciba_mode", cibaMode.name());

		callAndStopOnFailure(GenerateServerConfiguration.class);
		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(SetRsaAltServerJwks.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		callAndStopOnFailure(AddCibaTokenDeliveryModePollToTokenDeliveryModesSupported.class);
		if(isBrazil()) {
			callAndStopOnFailure(CheckCIBAModeIsPoll.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
			callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
		} else {
			callAndStopOnFailure(AddCibaTokenDeliveryModePingToTokenDeliveryModesSupported.class);
			callAndStopOnFailure(ExtractServerSigningAlg.class);
		}

		callAndStopOnFailure(AddIdTokenSigningAlgsToServerConfiguration.class);
		callAndStopOnFailure(AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration.class, "FAPI2-4.3.1-9");

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);

		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
			callAndStopOnFailure(BrazilAddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
		} else {
			callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
			callAndStopOnFailure(AddBackchannelAuthenticationRequestSigningAlgValuesSupportedToServer.class);
		}

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		if(isBrazil()) {
			expose("obtain_id_token", baseUrl + "/token/obtain");
			exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
			exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
		} else {
			exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
		}

		callAndStopOnFailure(CheckServerConfiguration.class);
		callAndStopOnFailure(CheckNotificationEndpointServerConfiguration.class, "CIBA-9");

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI1-BASE-5.2.2-5", "FAPI1-BASE-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		configureClient();

		onConfigurationCompleted();
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

		call(exec().unmapKey("client_request"));
		setStatus(Status.WAITING);

		if (startingShutdown) {
			throw new TestFailureException(
				getId(),
				"Client has incorrectly called '%s' after receiving a response that must cause it to stop interacting with the server".formatted(path)
			);
		}

		switch (path) {
			case ".well-known/openid-configuration":
				return discoveryEndpoint();
			case "jwks":
				return jwksEndpoint();
			case "token/obtain":
				return obtainIdToken();
			case "backchannel":
				if (ClientAuthType.MTLS.equals(clientAuthType)) {
					throw new TestFailureException(
						getId(),
						"In MTLS mode, the backchannel endpoint must be called over an mTLS secured connection using the backchannel_authentication_endpoint found in mtls_endpoint_aliases."
					);
				}
				return backchannelEndpoint(requestId);
			case "token":
				throw new TestFailureException(
					getId(),
					"Token endpoint must be called over an mTLS secured connection using the token_endpoint found in mtls_endpoint_aliases."
				);
			case "userinfo":
				return userinfoEndpoint(requestId);
			default:
				throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
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

		switch (path) {
			case "backchannel":
				return backchannelEndpoint(requestId);
			case "token":
				return tokenEndpoint(requestId);
			case ACCOUNTS_PATH:
			case FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH:
				return accountsEndpoint(requestId);
			case FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH:
				return brazilHandleNewConsentRequest(requestId, false);
			case FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH:
				return brazilHandleNewConsentRequest(requestId, true);
			case FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH:
				return brazilHandleNewPaymentInitiationRequest(requestId);
			default:
				if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
					return brazilHandleGetConsentRequest(requestId, path, false);
				}
				if(path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")) {
					return brazilHandleGetConsentRequest(requestId, path, true);
				}
				throw new TestFailureException(getId(), "Got unexpected HTTP (using mtls) call to " + path);
		}
	}

	private void exposeMtlsPath(String name, String path) {
		String baseUrlMtls = env.getString("base_mtls_url");
		env.putString(name, baseUrlMtls + "/" + path);
		exposeEnvString(name);
	}

	protected void checkMtlsCertificate() {
		callAndContinueOnFailure(ExtractClientCertificateFromRequestHeaders.class, ConditionResult.FAILURE);
		callAndStopOnFailure(CheckForClientCertificate.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-5");
		callAndContinueOnFailure(EnsureClientCertificateMatches.class, ConditionResult.FAILURE);
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

	protected void configureClient() {
		eventLog.startBlock("Verify configuration of client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks();
		validateClientConfiguration();

		eventLog.endBlock();
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
	}

	protected void validateClientJwks() {
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI1-BASE-5.2.4-2", "FAPI1-BASE-5.2.4-3");
	}

	protected Object discoveryEndpoint() {
		setStatus(Status.RUNNING);

		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	protected Object jwksEndpoint() {
		setStatus(Status.RUNNING);

		JsonObject jwks = env.getObject("server_public_jwks");
		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object tokenEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint").mapKey("token_endpoint_request", requestId));

		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		checkMtlsCertificate();
		call(sequence(validateTokenEndpointClientAuthenticationSteps));

		return handleTokenEndpointGrantType();
	}

	protected Object handleTokenEndpointGrantType(){
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw new TestFailureException(getId(), "Token endpoint body does not contain the mandatory 'grant_type' parameter");
		}

		switch (grantType) {
			case "client_credentials":
				if (isBrazil()) {
					callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
					return clientCredentialsGrantType();
				}
				break;
			case "refresh_token":
				return refreshTokenGrantType();
			case "urn:openid:params:grant-type:ciba":
				return cibaGrantType();
		}
		throw new TestFailureException(getId(), "Got an unexpected grant type on the token endpoint: " + grantType);
	}

	protected Object refreshTokenGrantType() {
		callAndStopOnFailure(ValidateRefreshToken.class);

		issueAccessToken();
		issueRefreshToken(); // rotate refresh token
		env.removeNativeValue("id_token");
		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());
		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected Object clientCredentialsGrantType() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		// this puts the client credentials specific token into its own box for later
		callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());
		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);
	}

	protected Object cibaGrantType() {
		callAndStopOnFailure(VerifyAuthReqId.class, ConditionResult.FAILURE, "CIBA-10.1");

		HttpStatus statusCode;

		if(VerifyAuthReqIdExpiration.isAuthReqIdExpired(env)) {
			callAndContinueOnFailure(VerifyAuthReqIdExpiration.class, ConditionResult.INFO);
			throw new TestFailureException(getId(), "expired_token", "The auth_req_id has expired. The client will need to make a new authentication request.");
		} else {
			callAndStopOnFailure(VerifyThatPollingIntervalIsRespected.class, ConditionResult.FAILURE, "CIBA-7.3");

			statusCode = createTokenEndpointResponseForCiba();
		}

		tokenEndpointCallComplete();

		call(exec().unmapKey("token_endpoint_request").endBlock());

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), statusCode);
	}

	private HttpStatus createTokenEndpointResponseForCiba() {
		callAndStopOnFailure(IncrementTokenEndpointPollCount.class);
		int tokenPollCount = env.getInteger("token_poll_count");
		if (clientWasPinged() || clientHasPolledEnough(tokenPollCount)) {
			issueAccessToken();
			issueRefreshToken();
			issueIdToken();

			createFinalTokenResponse();

			callAndContinueOnFailure(RedeemAuthReqId.class, ConditionResult.INFO);
			return HttpStatus.OK;
		} else {
			createIntermediateTokenResponse();
			return HttpStatus.BAD_REQUEST;
		}
	}

	// To facilitate id_token_hint testing
	private Object obtainIdToken() {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(GenerateIdTokenClaimsWith181DayExp.class);

		callAndStopOnFailure(profileSpecificSignIdToken);

		JsonObject response = new JsonObject();
		response.addProperty("id_token", env.getString("id_token"));

		env.removeObject("id_token_claims");
		env.removeObject("id_token");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected void issueIdToken() {
		prepareIdTokenClaims();
		signIdToken();
		encryptIdToken();
	}

	protected void issueAccessToken() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");
	}

	protected void issueRefreshToken() {
		callAndStopOnFailure(CreateRefreshToken.class);
	}

	protected void prepareIdTokenClaims() {

		env.mapKey("authorization_request_object", "backchannel_request_object");

		if(isBrazil()) {
			callAndStopOnFailure(GenerateIdTokenClaimsWith181DayExp.class);
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
		} else {
			callAndStopOnFailure(GenerateIdTokenClaims.class);
		}

		skipIfMissing(null, new String[] {"at_hash"}, ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		if(isBrazil()) {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				FAPIBrazilOBAddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		} else {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				AddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		}

		env.unmapKey("authorization_request_object");

	}

	protected void signIdToken() {
		callAndStopOnFailure(profileSpecificSignIdToken);
		addCustomSignatureOfIdToken();
	}

	/**
	 * This method does not actually encrypt id_tokens, even when id_token_encrypted_response_alg is set
	 * "5.2.3.1.  ID Token as detached signature" reads:
	 *  "5. shall support both signed and signed & encrypted ID Tokens."
	 *  So an implementation MUST support non-encrypted id_tokens too and we do NOT allow testers to run all tests with id_token
	 *  encryption enabled, encryption will be enabled only for certain tests and the rest will return non-encrypted id_tokens.
	 *  Second client will be used for encrypted id_token tests. First client does not need to have an encryption key
	 */
	protected void encryptIdToken() { }

	protected boolean clientHasPolledEnough(int tokenPollCount) {
		return tokenPollCount > 2;
	}

	private boolean clientWasPinged() {
		Boolean clientWasPinged = env.getBoolean("client_was_pinged");
		return CIBAMode.PING.equals(cibaMode) && clientWasPinged != null && clientWasPinged;
	}

	protected Object userinfoEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint").mapKey("incoming_request", requestId));

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

	protected ResponseEntity<?> backchannelEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("RP calls the backchannel endpoint").mapKey("backchannel_endpoint_http_request", requestId));

		call(sequence(VerifyPostedFormData.class));

		if(clientAuthType == ClientAuthType.MTLS || isBrazil()) {
			env.mapKey("token_endpoint_request", requestId);
			checkMtlsCertificate();
			env.unmapKey("token_endpoint_request");
		}
		call(sequence(validateBackchannelClientAuthenticationSteps));

		JsonObject httpRequestObj = env.getObject("backchannel_endpoint_http_request");
		env.putObject("backchannel_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));

		callAndStopOnFailure(EnsureBackchannelRequestObjectWasNotEncrypted.class, ConditionResult.FAILURE, "CIBA-7.1.1");
		callAndStopOnFailure(ExtractRequestObjectFromBackchannelEndpointRequest.class, "FAPI-CIBA-5.2.3.1");

		env.mapKey("authorization_request_object", "backchannel_request_object");
		validateRequestObjectForBackchannelEndpointRequest();
		env.unmapKey("authorization_request_object");

		callAndContinueOnFailure(EnsureBackchannelRequestParametersDoNotAppearOutsideJwt.class, ConditionResult.FAILURE, "CIBA-7.1.1");
		callAndContinueOnFailure(BackchannelRequestHasExactlyOneOfTheHintParameters.class, ConditionResult.FAILURE, "CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestRequestedExpiryIsAnInteger.class, ConditionResult.FAILURE,"CIBA-7.1", "CIBA-7.1.1");

		skipIfElementMissing("backchannel_request_object", "claims.id_token_hint", ConditionResult.SUCCESS, IdTokenIsSignedWithServerKey.class, ConditionResult.FAILURE, "CIBA-7.1");
		if(isBrazil()) {

			if(env.getElementFromObject("backchannel_request_object", "claims.id_token_hint") != null) {
				callAndStopOnFailure(ExtractIdTokenHintFromBackchannelEndpointRequest.class, ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
				env.mapKey("id_token", "id_token_hint");
				env.mapKey("authorization_endpoint_request", "backchannel_request_object");
				callAndContinueOnFailure(ValidateIdTokenHasRequiredBrazilHeaders.class, ConditionResult.FAILURE, "BrazilCIBA-5.2.2");
				call(new PerformStandardIdTokenChecks().replace(ValidateIdToken.class, condition(ValidateIdTokenExcludingIat.class)));
				callAndContinueOnFailure(VerifyIdTokenValidityIsMinimum180Days.class, ConditionResult.WARNING, "BrazilCIBA-5.2.2");
				env.unmapKey("authorization_endpoint_request");
				env.unmapKey("id_token");
			} else if (env.getElementFromObject("backchannel_request_object", "claims.login_hint") != null) {
				callAndContinueOnFailure(EnsureLoginHintEqualsConsentId.class, ConditionResult.FAILURE);
			} else {
				throw new TestFailureException(getId(), "Open Finance/Insurance Brazil requires either id_token_hint or login_hint.");
			}

			callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
		}

		HttpStatus httpStatus = createBackchannelResponse();
		if(CIBAMode.PING.equals(cibaMode)) {
			call(sequence(VerifyClientNotificationToken.class));
			spawnThreadForPing();
		}

		call(exec().unmapKey("backchannel_endpoint_http_request").endBlock());
		backchannelEndpointCallComplete();

		return new ResponseEntity<>(env.getObject("backchannel_endpoint_response"), httpStatus);
	}

	private void spawnThreadForPing() {
		getTestExecutionManager().runInBackground(() -> {
			int secondsUntilPing = 10;
			Thread.sleep(secondsUntilPing * 1000L);

			call(exec().startBlock("OP calls the client notification endpoint"));
			setStatus(Status.RUNNING);

			sendPingRequestAndVerifyResponse();

			call(exec().endBlock());
			setStatus(Status.WAITING);

			return "done";
		});
	}

	// This method is for the most part a copy of validateRequestObjectForAuthorizationEndpointRequest() in AbstractFAPI1AdvancedFinalClientTest.
	protected void validateRequestObjectForBackchannelEndpointRequest() {

		validateRequestObjectCommonChecks();

		callAndStopOnFailure(ValidateBackchannelRequestObjectClaims.class);
		callAndStopOnFailure(ValidateBackchannelRequestObjectSigningAlgMatchesSupported.class, "CIBA-4");

		env.mapKey("authorization_endpoint_http_request_params", "backchannel_endpoint_http_request_params");

		if(ClientAuthType.MTLS.equals(clientAuthType)) {
			callAndContinueOnFailure(EnsureRequiredBackchannelRequestParametersMatchRequestObject.class, ConditionResult.FAILURE, "OIDCC-6.1", "FAPI1-ADV-5.2.3-9");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

		callAndContinueOnFailure(CreateEffectiveAuthorizationRequestParameters.class, ConditionResult.WARNING);
		callAndStopOnFailure(ExtractRequestedScopes.class);

		if(isBrazil()) {
			callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				callAndStopOnFailure(EnsureScopeContainsPayments.class);
			} else {
				callAndStopOnFailure(EnsureScopeContainsAccounts.class);
			}
		} else {
			callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScopeDisregardingOrder.class);
		}

		callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI1-BASE-5.2.3-7");

		if(ClientAuthType.MTLS.equals(clientAuthType)) {
			// client_id is required as a parameter in MTLS mode
			callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");
		} else {
			skipIfElementMissing(
				CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
				CreateEffectiveAuthorizationRequestParameters.CLIENT_ID,
				ConditionResult.INFO, EnsureMatchingClientId.class, ConditionResult.FAILURE, "OIDCC-3.1.2.1");
		}

		env.unmapKey("authorization_endpoint_http_request_params");
	}

	protected void validateRequestObjectCommonChecks() {
		callAndStopOnFailure(FAPIValidateRequestObjectSigningAlg.class, "FAPI1-ADV-8.6");
		callAndContinueOnFailure(FAPIValidateRequestObjectMediaType.class, Condition.ConditionResult.WARNING, "JAR-4");
		callAndContinueOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, ConditionResult.INFO, "FAPI1-ADV-5.2.3-5", "OIDCC-5.5.1.1");
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI1-ADV-5.2.2-13");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-17");
		callAndContinueOnFailure(NonIssuerAsAudClaim.class, ConditionResult.WARNING, "CIBA-7.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.FAILURE, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.FAILURE, "JAR-10.8");
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI1-ADV-5.2.2-1");
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

		resourceEndpointCallComplete();

		return new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	protected void validateResourceEndpointHeaders() {
		skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-3");

		skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address", ConditionResult.INFO,
			ExtractFapiIpAddressHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-4");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");
	}

	protected void checkResourceEndpointRequest(boolean useClientCredentialsAccessToken) {
		callAndContinueOnFailure(EnsureBearerAccessTokenNotInParams.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-1");
		callAndContinueOnFailure(ExtractBearerAccessTokenFromHeader.class, ConditionResult.FAILURE,  "FAPI1-BASE-6.2.2-1");
		if(useClientCredentialsAccessToken) {
			callAndContinueOnFailure(RequireBearerClientCredentialsAccessToken.class, ConditionResult.FAILURE);
		} else {
			callAndContinueOnFailure(RequireBearerAccessToken.class, ConditionResult.FAILURE);
		}
		validateResourceEndpointHeaders();
	}

	protected void resourceEndpointCallComplete() {
		fireTestFinished();
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
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedPayments.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractPaymentsConsentRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
			callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1-4");
			callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);
			//ensure aud equals endpoint url	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, ConditionResult.FAILURE,"RFC7519-4.1.3", "BrazilOB-6.1-3");
			//ensure ISS equals TLS certificate organizational unit
			callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE,"BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
			//ensure jti is uuid	"BrazilOB-6.1"
			callAndContinueOnFailure(FAPIBrazilEnsureConsentRequestJtiIsUUIDv4.class, ConditionResult.FAILURE,"BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilValidateConsentRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");

			callAndContinueOnFailure(FAPIBrazilFetchClientOrganizationJwksFromDirectory.class, ConditionResult.FAILURE, "BrazilOB-6.1-6");
			env.mapKey("parsed_client_request_jwt", "new_consent_request");
			callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1-6");
			env.unmapKey("parsed_client_request_jwt");

		} else {
			callAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedConsents.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
		}

		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE,"FAPI1-BASE-6.2.1-11");

		ResponseEntity<Object> responseEntity = null;
		if(isPayments) {
			callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentsConsentResponse.class, ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
			callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE,"BrazilOB-6.1-2");
			String signedConsentResponse = env.getString("signed_consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);
		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateNewConsentResponse.class, ConditionResult.FAILURE,"BrazilOB-5.2.2.2");
			JsonObject response = env.getObject("consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");
			responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.CREATED);
		}
		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);

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
		callAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		String requestedConsentId = path.substring(path.lastIndexOf('/')+1);
		env.putString("requested_consent_id", requestedConsentId);

		ResponseEntity<Object> responseEntity = null;
		if(isPayments) {
			callAndContinueOnFailure(FAPIBrazilGenerateGetPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
			callAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-2");
			String signedConsentResponse = env.getString("signed_consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.OK);

		} else {
			callAndContinueOnFailure(FAPIBrazilGenerateGetConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
			JsonObject response = env.getObject("consent_response");
			JsonObject headerJson = env.getObject("consent_response_headers");
			responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.OK);
		}

		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);

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

		callAndContinueOnFailure(FAPIBrazilExtractPaymentInitiationRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
		env.mapKey("parsed_client_request_jwt", "payment_initiation_request");
		callAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1-6");
		env.unmapKey("parsed_client_request_jwt");

		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1-4");

		callAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);

		//ensure aud equals endpoint url	"BrazilOB-6.1"
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestAud.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
		//ensure ISS equals TLS certificate organizational unit
		callAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");
		callAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1-3");

		callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
		callAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-2");
		String signedConsentResponse = env.getString("signed_payment_initiation_response");
		JsonObject headerJson = env.getObject("payment_initiation_response_headers");

		HttpHeaders headers = headersFromJson(headerJson);
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		ResponseEntity<Object> responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);

		call(exec().unmapKey("incoming_request").endBlock());
		resourceEndpointCallComplete();

		return responseEntity;
	}

}
