package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.*;
import net.openid.conformance.condition.as.jarm.GenerateJARMResponseClaims;
import net.openid.conformance.condition.as.jarm.SendJARMResponseWitResponseModeQuery;
import net.openid.conformance.condition.as.jarm.SignJARMResponse;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.condition.as.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR;
import net.openid.conformance.condition.as.par.ExtractRequestObjectFromPAREndpointRequest;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.*;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.*;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	FAPIAuthRequestMethod.class,
	FAPIResponseMode.class,
	FAPIJARMType.class,
	CIBAMode.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
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
@VariantNotApplicable(parameter = CIBAMode.class, values = {
	"push"
})
public abstract class AbstractFAPICIBAID1ClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";
	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends Condition> addBackchannelEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	private Class<? extends ConditionSequence> configureAuthRequestMethodSteps;
	private Class<? extends ConditionSequence> configureResponseModeSteps;
	private Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	private Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;

	// Controls which endpoints we should expose to the client
	protected FAPI1FinalOPProfile profile;

	protected FAPIAuthRequestMethod authRequestMethod;

	protected FAPIResponseMode responseMode;

	protected ClientAuthType clientAuthType;

	protected FAPIJARMType jarmType;

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
		String baseUrlMtls = env.getString("base_url").replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		env.putString(name, baseUrlMtls + "/" + path);
		exposeEnvString(name);
	}

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken(){}

	protected void endTestIfRequiredParametersAreMissing(){}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		profile = getVariant(FAPI1FinalOPProfile.class);
		authRequestMethod = getVariant(FAPIAuthRequestMethod.class);
		responseMode = getVariant(FAPIResponseMode.class);
		clientAuthType = getVariant(ClientAuthType.class);
		jarmType = getVariant(FAPIJARMType.class);

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			//https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-dynamic-client-registration-1_ID1.html#name-authorization-server
			// shall advertise mtls_endpoint_aliases as per clause 5 RFC 8705 OAuth 2.0 Mutual-TLS Client Authentication and
			// Certificate-Bound Access Tokens the token_endpoint, registration_endpoint and userinfo_endpoint;
			callAndStopOnFailure(FAPICIBAID1GenerateServerConfiguration.class);
		} else {
			// We should really create the 'Brazil' configuration that contains mtls_endpoint_aliases in at least some
			// cases - it's mandatory for clients to support it as per https://datatracker.ietf.org/doc/html/rfc8705#section-5
			callAndStopOnFailure(GenerateServerConfigurationMTLS.class);
		}

		//this must come before configureResponseModeSteps due to JARM signing_algorithm dependency
		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		if(true) { // TODO: WAS: profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
			callAndStopOnFailure(FAPICIBAID1SetGrantTypesSupportedInServerConfiguration.class, "BrazilOB-5.2.3-5");
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
			callAndStopOnFailure(FAPICIBAID1AddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
		} else {
			callAndStopOnFailure(ExtractServerSigningAlg.class);
		}

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);
		callAndStopOnFailure(addBackchannelEndpointAuthMethodSupported);

		if(configureAuthRequestMethodSteps!=null) {
			call(sequence(configureAuthRequestMethodSteps));
		}

		if(configureResponseModeSteps!=null) {
			call(sequence(configureResponseModeSteps));
		}
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		} else {
			callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		}

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");


		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			exposeMtlsPath("accounts_endpoint", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH);
			exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
			exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
			exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
		} else {
			exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
			exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);
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

	}

	protected void configureClients()
	{
		eventLog.startBlock("Verify configuration of first client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks(false);
		validateClientConfiguration();

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
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI1-BASE-5.2.4-2", "FAPI1-BASE-5.2.4-3");
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

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.FAILURE, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

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
		} else if (path.equals("backchannel")) {
			return backchannelEndpoint(requestId);
		} else if (path.equals("token")) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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
			return userinfoEndpoint(requestId);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			if(startingShutdown){
				throw new TestFailureException(getId(), "Client has incorrectly called '" + path + "' after receiving a response that must cause it to stop interacting with the server");
			}
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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
			return accountRequestsEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.FAILURE, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5-1");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH) || path.equals(FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH)) {
			return accountsEndpoint(requestId);
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			return parEndpoint(requestId);
		}
		if (profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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
		skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-3");

		skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address", ConditionResult.INFO,
			ExtractFapiIpAddressHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-4");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ExtractFapiInteractionIdHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");

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
			callAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, ConditionResult.FAILURE,"BrazilOB-6.1-3");
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
			if(isPayments) {
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			}
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
			if(isPayments) {
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			}
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


		ResponseEntity<Object> responseEntity = null;
		callAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2.2");
		callAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-2");
		String signedConsentResponse = env.getString("signed_payment_initiation_response");
		JsonObject headerJson = env.getObject("payment_initiation_response_headers");

		HttpHeaders headers = headersFromJson(headerJson);
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
		responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

		callAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);

		call(exec().unmapKey("incoming_request").endBlock());
		resourceEndpointCallComplete();

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
		callAndContinueOnFailure(ExtractClientCertificateFromTokenEndpointRequestHeaders.class, ConditionResult.FAILURE);
		callAndStopOnFailure(CheckForClientCertificate.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-5");
		callAndContinueOnFailure(EnsureClientCertificateMatches.class, ConditionResult.FAILURE);
	}
	protected void authenticateParEndpointRequest(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));

		if(clientAuthType == ClientAuthType.MTLS || profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			// there is no requirement to present an MTLS certificate at the PAR endpoint when using private_key_jwt.
			// (This differs to the token endpoint, where an MTLS certificate must always be presented, as one is
			// required to bind the issued access token to.)
			// The exception is Brazil, where a TLS client certificate must be presented to all endpoints in all cases.
			checkMtlsCertificate();
		}

		if(clientAuthType == ClientAuthType.PRIVATE_KEY_JWT) {
			call(new ValidateClientAuthenticationWithPrivateKeyJWT().
				replace(ValidateClientAssertionClaims.class, condition(ValidateClientAssertionClaimsForPAREndpoint.class).requirements("PAR-2"))
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

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI1-BASE-5.2.3.1-1");

		callAndStopOnFailure(FilterUserInfoForScopes.class);
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
		}

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	protected Object jwksEndpoint() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object tokenEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint")
			.mapKey("token_endpoint_request", requestId));

		callAndStopOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		checkMtlsCertificate();

		call(sequence(validateClientAuthenticationSteps));

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else if (grantType.equals("client_credentials")) {
			if( profile == FAPI1FinalOPProfile.OPENBANKING_UK) {
				// we're doing the client credentials grant for initial token access
				return clientCredentialsGrantType(requestId);
			} else if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		// this puts the client credentials specific token into its own box for later
		callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected void validateRedirectUriForAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateRedirectUri.class);
	}

	protected Object authorizationCodeGrantType(String requestId) {

		callAndStopOnFailure(ValidateAuthorizationCode.class);

		validateRedirectUriForAuthorizationCodeGrantType();

		if(authRequestMethod==FAPIAuthRequestMethod.PUSHED) {
			callAndStopOnFailure(ValidateCodeVerifierWithS256.class, "RFC7636-4.6", "FAPI1-ADV-5.2.3-15");
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

	protected ResponseEntity<?> backchannelEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		Command backchannelBlock = exec().startBlock("RP calls the backchannel endpoint");
		backchannelBlock.mapKey("backchannel_endpoint_http_request", requestId);
		call(backchannelBlock);

		callAndContinueOnFailure(BackchannelRequestIsPostedCondition.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestIsFormDataCondition.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");

		// TODO: Enable this one again, only problem is that the condition sequence wants token_endpoint_request, so it must be reworked
		// call(sequence(validateClientAuthenticationSteps));

		callAndContinueOnFailure(BackchannelRequestHasHintCondition.class, Condition.ConditionResult.FAILURE, "CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestHasScopeCondition.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");
		callAndContinueOnFailure(BackchannelRequestRequestedExpiryCondition.class, Condition.ConditionResult.FAILURE,"CIBA-7.1");

		JsonObject backchannelResponse = new JsonObject();
		String authReqId = RFC6749AppendixASyntaxUtils.generateVSChar(40, 10, 0);
		backchannelResponse.addProperty("auth_req_id", authReqId);
		backchannelResponse.addProperty("interval", 5);

		String requestedExpiryString = env.getString("backchannel_endpoint_http_request", "body_form_params.requested_expiry");
		int expiresIn = getIntValueOrDefault(requestedExpiryString, 180);
		backchannelResponse.addProperty("expires_in", expiresIn);

		call(exec().unmapKey("backchannel_endpoint_http_request").endBlock());

		return new ResponseEntity<>(backchannelResponse, HttpStatus.OK);
	}

	protected static int getIntValueOrDefault(String intString, int defaultValue) {
		try {
			return Integer.parseInt(intString);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	@UserFacing
	protected Object authorizationEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();
		if(authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			callAndStopOnFailure(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR.class);
		}

		if(authRequestMethod == FAPIAuthRequestMethod.BY_VALUE) {
			callAndStopOnFailure(ExtractRequestObject.class, "FAPI1-ADV-5.2.2-10");
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndStopOnFailure(EnsureRequestObjectWasEncrypted.class, "BrazilOB-5.2.3-3");
				callAndStopOnFailure(FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM.class, "BrazilOB-6.1.1-1");
			}
		}

		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class);

		endTestIfRequiredParametersAreMissing();

		validateRequestObjectForAuthorizationEndpointRequest();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			if(jarmType==FAPIJARMType.PLAIN_OAUTH) {
				throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
			}
			callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, "FAPI1-BASE-5.2.2.2");
		} else {
			if(jarmType==FAPIJARMType.OIDC) {
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
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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
		if(jarmType==FAPIJARMType.OIDC) {
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndContinueOnFailure(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class, ConditionResult.FAILURE,
					"FAPI1-ADV-5.2.3-5", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.4");
			} else {
				callAndContinueOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, ConditionResult.INFO,
					"FAPI1-ADV-5.2.3-5", "OIDCC-5.5.1.1");
			}
		}
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI1-ADV-5.2.2-13");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-17");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, ConditionResult.WARNING, "OIDCC-13.3");
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

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
			Boolean wasInitialConsentRequestToPaymentsEndpoint = env.getBoolean("payments_consent_endpoint_called");
			if(wasInitialConsentRequestToPaymentsEndpoint) {
				callAndStopOnFailure(EnsureScopeContainsPayments.class);
			} else {
				callAndStopOnFailure(EnsureScopeContainsAccounts.class);
			}
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
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
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

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				FAPIBrazilAddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
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

		if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeFragment.class, "OIDCC-3.3.2.5");
		}
		if(responseMode==FAPIResponseMode.JARM) {
			createJARMResponse();
			//send via redirect
			callAndStopOnFailure(SendJARMResponseWitResponseModeQuery.class, "OIDCC-3.3.2.5", "JARM-4.3.1");
		}

		exposeEnvString("authorization_endpoint_response_redirect");
	}

	protected void createJARMResponse() {
		generateJARMResponseClaims();
		//authorization_signed_response_alg will not be taken into account. signing_algorithm will be used
		callAndStopOnFailure(SignJARMResponse.class,"JARM-4.2");
		encryptJARMResponse();
	}

	protected void encryptJARMResponse() {
		skipIfElementMissing("client", "authorization_encrypted_response_alg", ConditionResult.INFO,
			EncryptJARMResponse.class, ConditionResult.FAILURE, "JARM-5");

	}

	protected void generateJARMResponseClaims() {
		callAndStopOnFailure(GenerateJARMResponseClaims.class,"JARM-4.1.1");
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

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointAuthMethodSupported = AddTLSClientAuthToServerConfiguration.class;
		addBackchannelEndpointAuthMethodSupported = FAPICIBAID1AddTLSClientAuthToServerConfiguration.class;
		// TODO: Enable this one again, only problem is that the condition sequence wants token_endpoint_request, so it must be reworked
		//validateClientAuthenticationSteps = ValidateClientAuthenticationWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		addBackchannelEndpointAuthMethodSupported = FAPICIBAID1SetBackchannelEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		// TODO: Enable this one again, only problem is that the condition sequence wants token_endpoint_request, so it must be reworked
		// validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
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

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		//authorizationCodeGrantTypeProfileSteps = null;
		//authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = GenerateOpenBankingBrazilAccountsEndpointResponse.class;
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
