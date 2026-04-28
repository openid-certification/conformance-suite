package net.openid.conformance.vp1finalverifier;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CheckDCQLQueryCredentialFormatMatchesTestConfiguration;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckForUnexpectedPropertiesInVerifierInfo;
import net.openid.conformance.condition.as.CheckNoClientIdSchemeParameter;
import net.openid.conformance.condition.as.CheckNoPresentationDefinitionInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckNoRedirectUriInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckNoScopeParameter;
import net.openid.conformance.condition.as.CheckNoTransactionDataInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckRequestUriMethodParameter;
import net.openid.conformance.condition.as.CheckVerifierInfoInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateMDocGeneratedNonce;
import net.openid.conformance.condition.as.CreateMdocCredential;
import net.openid.conformance.condition.as.CreateSdJwtKbCredential;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureClientIdMatchesResponseUri;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureNoWalletNonceInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequestUriHasNoFragment;
import net.openid.conformance.condition.as.EnsureRequestUriIsHttps;
import net.openid.conformance.condition.as.EnsureResponseTypeIsVpToken;
import net.openid.conformance.condition.as.EnsureValidResponseUriForAuthorizationEndpointRequest;
import net.openid.conformance.condition.as.ExtractAndValidateX509HashClientId;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.FetchRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.as.OID4VPSetClientIdToIncludeClientIdScheme;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SetRequestUriParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.VP1FinalCheckForKeyIdInClientMetadataJWKs;
import net.openid.conformance.condition.as.VP1FinalCheckForUnexpectedParametersInVpClientMetadata;
import net.openid.conformance.condition.as.VP1FinalEncryptVPResponse;
import net.openid.conformance.condition.as.VP1FinalValidateClientMetadataJwksForEncryptedResponse;
import net.openid.conformance.condition.as.VP1FinalValidateVpFormatsSupportedInClientMetadata;
import net.openid.conformance.condition.as.ValidateDirectPostResponse;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRequestObjectAudForVP;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectIssIfPresent;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignatureAgainstX5cHeader;
import net.openid.conformance.condition.as.ValidateRequestObjectTypIsOAuthQauthReqJwt;
import net.openid.conformance.condition.as.ValidateResponseMode;
import net.openid.conformance.condition.as.ValidateVpClientMetadataEncryptionForHaip;
import net.openid.conformance.condition.as.ValidateVpClientMetadataJwksKeysArePublic;
import net.openid.conformance.condition.as.WarnIfRequestUriMethodInRequestObject;
import net.openid.conformance.condition.client.BuildUnsignedRequestToDirectPostEndpoint;
import net.openid.conformance.condition.client.CallDirectPostEndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInDcqlQuery;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted;
import net.openid.conformance.condition.client.CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ValidateDCQLQuery;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;


@VariantParameters({
	VPProfile.class,
	VP1FinalVerifierCredentialFormat.class,
	VP1FinalVerifierClientIdPrefix.class,
	VP1FinalVerifierResponseMode.class,
	VP1FinalVerifierRequestMethod.class
})
@VariantConfigurationFields(parameter = VP1FinalVerifierClientIdPrefix.class, value = "x509_san_dns", configurationFields = {
	"client.client_id"
})
@VariantNotApplicableWhen(
	parameter = VP1FinalVerifierResponseMode.class,
	values = {"direct_post"},  // unencrypted mode not applicable for HAIP
	whenParameter = VPProfile.class,
	hasValues = "haip"
)
public abstract class AbstractVP1FinalVerifierTest extends AbstractTestModule {
	protected VP1FinalVerifierClientIdPrefix clientIdPrefix;
	protected VP1FinalVerifierResponseMode responseMode;
	protected VP1FinalVerifierRequestMethod clientRequestType;

	protected boolean receivedAuthorizationRequest;
	protected boolean testFinished = false;

	/**
	 * for how long the test will wait for negative tests
	 */
	protected int waitTimeoutSeconds = 5;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}

		if(config.has("waitTimeoutSeconds")) {
			waitTimeoutSeconds = OIDFJSON.getInt(config.get("waitTimeoutSeconds"));
		}

		responseMode = getVariant(VP1FinalVerifierResponseMode.class);
		env.putString("response_mode", responseMode.toString());

		clientIdPrefix = getVariant(VP1FinalVerifierClientIdPrefix.class);
		env.putString("client_id_scheme", clientIdPrefix.toString());

		clientRequestType = getVariant(VP1FinalVerifierRequestMethod.class);

		env.putString("credential_format", getVariant(VP1FinalVerifierCredentialFormat.class).toString());

		configureServerConfiguration();

		String authz = env.getString("server", "authorization_endpoint");
		env.putString("authorization_endpoint", authz);
		exposeEnvString("authorization_endpoint");

		onServerConfigurationCompleted();

		configureServerJWKS();

		validateConfiguredServerJWKS();

		configureClientConfiguration();

		onBeforeFireSetupDone();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	protected void endTestIfRequiredAuthorizationRequestParametersAreMissing() {

	}

	/**
	 * called right before fireSetupDone is called
	 */
	protected void onBeforeFireSetupDone() {

	}

	protected void validateConfiguredServerJWKS() {
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
	}

	/**
	 * expected to add discoveryUrl and issuer to env
	 */
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfiguration.class);
	}

	protected void onServerConfigurationCompleted() {
		//fapi would call callAndStopOnFailure(CheckServerConfiguration.class); here
		switch(clientRequestType) {
			case URL_QUERY:
				// parameters passed directly in URL query, no request_uri support needed
				break;
			case REQUEST_URI_SIGNED:
				callAndStopOnFailure(SetRequestUriParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.2");
				break;
		}
	}

	/**
	 * override to modify the generated jwks
	 */
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKs.class);
	}

	protected void configureClientConfiguration() {
		switch (clientIdPrefix) {
			case X509_HASH -> {
				// there's only one possible client id for any given x5c certificate so create it later
			}
			case X509_SAN_DNS -> {
				callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
				callAndStopOnFailure(OID4VPSetClientIdToIncludeClientIdScheme.class, "OID4VP-1FINAL-5.9.3");
			}
			case REDIRECT_URI -> {
				// client_id equals the response_uri for this scheme; validated dynamically below
			}
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		// nothing to do here
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {
		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		Object responseObject = handleClientRequestForPath(requestId, path, servletResponse);

		if (finishTestIfAllRequestsAreReceived()) {
			fireTestFinished();
		} else {
			setStatus(Status.WAITING);
		}

		return responseObject;
	}
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse){

		if (path.equals("authorize")) {
			receivedAuthorizationRequest = true;
			return handleAuthorizationEndpointRequest(requestId);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	/**
	 * @return true if fireTestFinished should be called
	 */
	protected boolean finishTestIfAllRequestsAreReceived() {
		return testFinished;
	}

	protected void fetchAndProcessRequestUri() {
		callAndStopOnFailure(FetchRequestUriAndExtractRequestObject.class, "JAR-5.2.3");
		callAndContinueOnFailure(EnsureRequestUriIsHttps.class, ConditionResult.FAILURE, "JAR-5.2");
		callAndContinueOnFailure(EnsureRequestUriHasNoFragment.class, ConditionResult.FAILURE);
	}

	protected void extractAuthorizationEndpointRequestParameters() {
		if(clientRequestType == VP1FinalVerifierRequestMethod.REQUEST_URI_SIGNED) {
			fetchAndProcessRequestUri();
//		} else if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {
//			callAndStopOnFailure(ExtractRequestObject.class, "OIDCC-6.1");
//		} else {
//			//handle plain http request case
//			callAndStopOnFailure(EnsureRequestDoesNotContainRequestObject.class, "OIDCC-6.1");
		}

		if(clientRequestType == VP1FinalVerifierRequestMethod.REQUEST_URI_SIGNED) {
			switch (clientIdPrefix) {
				case X509_HASH -> {
					callAndContinueOnFailure(ExtractAndValidateX509HashClientId.class, ConditionResult.FAILURE);
				}
				case X509_SAN_DNS, REDIRECT_URI -> {}
			}
			validateRequestObject();
			callAndStopOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class);
			skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class,
										ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest.class, ConditionResult.WARNING);

		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class, "OIDCC-6.1", "OIDCC-6.2");

		extractNonceFromAuthorizationEndpointRequestParameters();

		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CODE_CHALLENGE, ConditionResult.INFO, EnsureAuthorizationRequestContainsPkceCodeChallenge.class, ConditionResult.FAILURE, "RFC7636-4.3");
	}

	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.2");
		// nonce checks added in VP1FinalVerifierHappyFlow
	}

	protected void validateAuthorizationEndpointRequestParameters() {

		//  "response_uri": "https://funke.animo.id/siop/019368ed-3787-7669-b7f4-8c012238e90d/authorize",
		//    "iss": "https://funke.animo.id/siop/019368ed-3787-7669-b7f4-8c012238e90d/authorize",
		//    "aud": "https://self-issued.me/v2",
		//    "nbf": 1732707945,
		//    "presentation_definition": {
		//    },
		//    "state": "1021415019920846486075038",
		//    "exp": 1732708065,
		//    "iat": 1732707945,
		//    "client_metadata": {
		//      "jwks": {
		//        "keys": [
		//          {
		//            "use": "enc",
		//            "kty": "EC",
		//            "crv": "P-256",
		//            "x": "_SlKY_V2SpmRPHI7zQNDcSLKRyvI1_k3SMh7XF-kgeM",
		//            "y": "MOAKQxM7pA9dcrqGyP8WoLvk0hxqk_p71Pm_HFY0cj8",
		//            "kid": "zDnaezhX5hvd8qAWj7hy7WRX2tM79F4fF3XGgx6V9rxa7zWH8"
		//          }
		//        ]
		//      },
		//      "authorization_encrypted_response_alg": "ECDH-ES",
		//      "authorization_encrypted_response_enc": "A256GCM",
		//      "logo_uri": "https://funke.animo.id/assets/verifiers/bunde.png",
		//      "client_name": "Die Bundesregierung",
		//      "client_id": "funke.animo.id",
		//      "passBy": "VALUE",
		//      "response_types_supported": [
		//        "vp_token"
		//      ],
		//      "subject_syntax_types_supported": [
		//        "urn:ietf:params:oauth:jwk-thumbprint",
		//        "did:web",
		//        "did:key",
		//        "did:jwk"
		//      ],
		//      "vp_formats": {
		//      }
		//    },
		//    "jti": "d18030b0-55c7-4a69-afc8-ff8e05337f4f"
		//  }
		//}
		callAndContinueOnFailure(EnsureResponseTypeIsVpToken.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMode.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckNoClientIdSchemeParameter.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckNoScopeParameter.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
		callAndContinueOnFailure(CheckRequestUriMethodParameter.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
		skipIfMissing(null, new String[]{"authorization_request_object"}, ConditionResult.INFO,
			WarnIfRequestUriMethodInRequestObject.class, ConditionResult.WARNING, "OID4VP-1FINAL-5.1");
		// wallet_nonce is a request-object claim that the verifier should only emit when responding
		// to a wallet POST that included wallet_nonce. In POST mode, VP1FinalVerifierRequestUriMethodPost
		// runs EnsureWalletNonceClaimMatchesPostedValue to verify the value; here we ensure it is absent.
		String requestUriMethod = env.getString("authorization_endpoint_http_request_params", "request_uri_method");
		if (!"post".equals(requestUriMethod)) {
			skipIfMissing(null, new String[]{"authorization_request_object"}, ConditionResult.INFO,
				EnsureNoWalletNonceInRequestObject.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.10");
		}
		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationRequest.class, ConditionResult.WARNING);
		callAndContinueOnFailure(CheckNoTransactionDataInVpAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5", "OID4VP-1FINAL-5.1", "OID4VP-1FINAL-8.4");
		callAndContinueOnFailure(CheckVerifierInfoInVpAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
		callAndContinueOnFailure(CheckForUnexpectedPropertiesInVerifierInfo.class, ConditionResult.WARNING, "OID4VP-1FINAL-5.1");

		switch (clientIdPrefix) {
			case X509_SAN_DNS -> {
				callAndContinueOnFailure(EnsureMatchingClientId.class, ConditionResult.FAILURE,"OIDCC-3.1.2.1");
			}
			case X509_HASH -> {
				// client id was checked earlier in ExtractAndValidateX509HashClientId
			}
			case REDIRECT_URI -> {
				callAndContinueOnFailure(EnsureClientIdMatchesResponseUri.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.9.2");
				// Store the client_id from the request so downstream conditions (e.g.
				// EnsureValidResponseUriForAuthorizationEndpointRequest) that require "client" work.
				String clientId = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_id");
				if (clientId != null) {
					env.putString("client", "client_id", clientId);
					env.putString("client_id", clientId);
				}
			}
		}

		// check redirect uri not present

		callAndContinueOnFailure(EnsureValidResponseUriForAuthorizationEndpointRequest.class, ConditionResult.FAILURE,"OID4VP-1FINAL-8.2");
		callAndContinueOnFailure(CheckNoRedirectUriInVpAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
		callAndContinueOnFailure(CheckNoPresentationDefinitionInVpAuthorizationRequest.class, ConditionResult.WARNING);

		callAndContinueOnFailure(VP1FinalCheckForUnexpectedParametersInVpClientMetadata.class, ConditionResult.WARNING, "OID4VP-1FINAL-5.1");
		callAndContinueOnFailure(VP1FinalValidateVpFormatsSupportedInClientMetadata.class, ConditionResult.FAILURE, "OID4VP-1FINALA-B.2.2", "OID4VP-1FINALA-B.3.4");

		switch (responseMode) {
			case DIRECT_POST_JWT:
				callAndContinueOnFailure(VP1FinalCheckForKeyIdInClientMetadataJWKs.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
				callAndContinueOnFailure(VP1FinalValidateClientMetadataJwksForEncryptedResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.3");
				callAndContinueOnFailure(ValidateVpClientMetadataJwksKeysArePublic.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
				if (getVariant(VPProfile.class) == VPProfile.HAIP) {
					callAndContinueOnFailure(ValidateVpClientMetadataEncryptionForHaip.class, ConditionResult.FAILURE, "HAIP-5-5", "OID4VP-1FINAL-8.3");
				}
				break;
			case DIRECT_POST:
				break;
		}

		endTestIfRequiredAuthorizationRequestParametersAreMissing();
	}

	protected void validateRequestObject() {
		callAndContinueOnFailure(ValidateRequestObjectTypIsOAuthQauthReqJwt.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5");
		skipIfElementMissing("authorization_request_object", "claims.exp", ConditionResult.INFO,
			OIDCCValidateRequestObjectExp.class, ConditionResult.FAILURE, "RFC7519-4.1.4");
		callAndContinueOnFailure(ValidateRequestObjectIat.class, ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(ValidateRequestObjectMaxAge.class, ConditionResult.FAILURE, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.WARNING, "JAR-10.8");

		// OID4VP section 5: the iss claim MAY be present in the Request Object, but wallets MUST ignore it.
		// If present, warn if it doesn't match client_id as it may indicate a verifier misconfiguration.
		callAndContinueOnFailure(ValidateRequestObjectIssIfPresent.class, ConditionResult.WARNING, "OID4VP-1FINAL-5");

		callAndContinueOnFailure(ValidateRequestObjectAudForVP.class, ConditionResult.WARNING, "OID4VP-1FINAL-5.8");

		callAndContinueOnFailure(ValidateRequestObjectSignatureAgainstX5cHeader.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.9.3");
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

	protected String getAuthorizationEndpointBlockText() {
		return "Authorization endpoint";
	}

	@UserFacing
	protected Object handleAuthorizationEndpointRequest(String requestId) {

		call(exec().startBlock(getAuthorizationEndpointBlockText()).mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();

		extractAuthorizationEndpointRequestParameters();

		validateAuthorizationEndpointRequestParameters();

		callAndStopOnFailure(ExtractDCQLQueryFromAuthorizationRequest.class, "OID4VP-1FINAL-6");
		callAndContinueOnFailure(ValidateDCQLQuery.class, ConditionResult.FAILURE, "OID4VP-1FINAL-6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInDcqlQuery.class, ConditionResult.WARNING, "OID4VP-1FINAL-6");
		// Test harness check: ensures verifier requests the credential format matching the test configuration
		callAndContinueOnFailure(CheckDCQLQueryCredentialFormatMatchesTestConfiguration.class, ConditionResult.FAILURE);

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		switch (getVariant(VP1FinalVerifierCredentialFormat.class)) {
			case SD_JWT_VC -> {
				createSdJwtCredential();
			}
			case ISO_MDL -> {
				callAndStopOnFailure(CreateMDocGeneratedNonce.class);
				createIsoMdlSessionTranscript();
				callAndStopOnFailure(CreateMdocCredential.class);
			}
		}
		callAndStopOnFailure(AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams.class, "OID4VP-1FINAL-8.1");

		customizeAuthorizationEndpointResponseParams();

		sendAuthorizationResponseToResponseUri();

		Object viewToReturn;

		String redirectTo = env.getString("direct_post_response", "body_json.redirect_uri");
		if (redirectTo != null) {
			viewToReturn = new RedirectView(redirectTo, false, false, false);
		} else {
			viewToReturn = new ModelAndView("resultCaptured",
				ImmutableMap.of(
					"returnUrl", "/log-detail.html?log=" + getId()
				));
		}

		testFinished = true;

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return viewToReturn;
	}

	protected void createSdJwtCredential() {
		callAndStopOnFailure(CreateSdJwtKbCredential.class);
	}

	protected void createIsoMdlSessionTranscript() {
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted.class);
				break;
			case DIRECT_POST_JWT:
				callAndStopOnFailure(CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted.class);
				break;
		}
	}

	protected void sendAuthorizationResponseToResponseUri() {
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(BuildUnsignedRequestToDirectPostEndpoint.class);
				break;
			case DIRECT_POST_JWT:
				callAndStopOnFailure(VP1FinalEncryptVPResponse.class);
				break;
		}
		callAndStopOnFailure(CallDirectPostEndpoint.class);

		call(exec().mapKey("endpoint_response", "direct_post_response"));
		validateDirectPostEndpointResponse();
	}

	protected void validateDirectPostEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
		callAndContinueOnFailure(ValidateDirectPostResponse.class, ConditionResult.WARNING, "OID4VP-1FINAL-8.2");
	}

	/**
	 * Called right before the response is generated
	 * Override to customize response parameters
	 */
	protected void customizeAuthorizationEndpointResponseParams() {

	}

	/**
	 * Only use in tests that need to wait for a timeout
	 * As the client hasn't called an endpoint after waitTimeoutSeconds (from configuration) seconds,
	 * assume it has correctly detected the error and aborted.
	 */
	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(waitTimeoutSeconds * 1000L);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestFinished();
			}
			return "done";
		});
	}
}
