package net.openid.conformance.vpid2verifier;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddPeVpTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddPresentationSubmissionToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInClaimsParameter;
import net.openid.conformance.condition.as.CheckForUnexpectedOpenIdClaims;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpClientMetadata;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterMemberValues;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterValues;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateIsoMdocPresentationSubmission;
import net.openid.conformance.condition.as.CreateMDocGeneratedNonce;
import net.openid.conformance.condition.as.CreateMdocCredential;
import net.openid.conformance.condition.as.CreateVPID2SdJwtPresentationSubmission;
import net.openid.conformance.condition.as.CreateVPID2SdJwtVpToken;
import net.openid.conformance.condition.as.CreateWalletIsoMdlAnnexBSessionTranscript;
import net.openid.conformance.condition.as.EncryptVPResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequestUriIsHttps;
import net.openid.conformance.condition.as.EnsureResponseTypeIsVpToken;
import net.openid.conformance.condition.as.EnsureValidResponseUriForAuthorizationEndpointRequest;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.FetchRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SetRequestUriParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.ValidateClientIdScheme;
import net.openid.conformance.condition.as.ValidateDirectPostResponse;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignatureAgainstX5cHeader;
import net.openid.conformance.condition.as.ValidateResponseMode;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.ValidateClientGrantTypes;
import net.openid.conformance.condition.as.dynregistration.ValidateClientLogoUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientPolicyUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientSubjectType;
import net.openid.conformance.condition.as.dynregistration.ValidateClientTosUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientUris;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultAcrValues;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestUris;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.client.BuildUnsignedRequestToDirectPostEndpoint;
import net.openid.conformance.condition.client.CallDirectPostEndpoint;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.VPID2VerifierClientIdScheme;
import net.openid.conformance.variant.VPID2VerifierCredentialFormat;
import net.openid.conformance.variant.VPID2VerifierRequestMethod;
import net.openid.conformance.variant.VPID2VerifierResponseMode;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;


@VariantParameters({
	VPID2VerifierCredentialFormat.class,
	VPID2VerifierClientIdScheme.class,
	VPID2VerifierResponseMode.class,
	VPID2VerifierRequestMethod.class
})
public abstract class AbstractVPID2VerifierTest extends AbstractTestModule {
	protected VPID2VerifierClientIdScheme clientIdScheme;
	protected VPID2VerifierResponseMode responseMode;
	protected VPID2VerifierRequestMethod clientRequestType;

	protected boolean receivedAuthorizationRequest;
	protected boolean testFinished = false;

	/**
	 * for how long the test will wait for negative tests
	 */
	protected int waitTimeoutSeconds = 5;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
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

		responseMode = getVariant(VPID2VerifierResponseMode.class);
		env.putString("response_mode", responseMode.toString());

		clientIdScheme = getVariant(VPID2VerifierClientIdScheme.class);
		env.putString("client_id_scheme", clientIdScheme.toString());

		clientRequestType = getVariant(VPID2VerifierRequestMethod.class);

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
//			case REQUEST_OBJECT:
//				callAndStopOnFailure(SetRequestParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.1");
//				callAndStopOnFailure(OIDCCAddRequestObjectSigningAlgValuesSupportedToServerConfiguration.class, "OIDCC-6.1");
//				break;
			case REQUEST_URI_SIGNED:
				callAndStopOnFailure(SetRequestUriParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.2");
				break;
//			case PLAIN_HTTP_REQUEST:
//				// nothing to do
//				break;
		}
	}

	/**
	 * override to modify the generated jwks
	 */
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKs.class);
	}

	protected void configureClientConfiguration() {
		callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
		processAndValidateClientJwks();
		validateClientMetadata();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		// nothing to do here
		setStatus(Status.WAITING);
	}

	/**
	 * Override to randomize jwks path
	 * @return
	 */
	protected String getJwksPath() {
		return "jwks";
	}


	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {
		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

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

	/**
	 * jwks and jwks_uri will be validated in validateClientJwks
	 */
	protected void validateClientMetadata() {
		callAndContinueOnFailure(ValidateClientGrantTypes.class, ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(ValidateClientLogoUris.class, ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientUris.class, ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientPolicyUris.class, ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientTosUris.class, ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, ConditionResult.FAILURE,"OIDCR-2");
		skipIfElementMissing("client", "id_token_signed_response_alg", ConditionResult.INFO,
			ValidateIdTokenSignedResponseAlg.class, ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, ConditionResult.FAILURE,"OIDCR-2");

		//userinfo
		skipIfElementMissing("client", "userinfo_signed_response_alg", ConditionResult.INFO,
			ValidateUserinfoSignedResponseAlg.class, ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, ConditionResult.FAILURE,"OIDCR-2");

		//request object
		skipIfElementMissing("client", "request_object_signing_alg", ConditionResult.INFO,
			ValidateRequestObjectSigningAlg.class, ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, ConditionResult.FAILURE,"OIDCR-2");

		//not validating token_endpoint_auth_method as we will override it anyway

		skipIfElementMissing("client", "token_endpoint_auth_signing_alg", ConditionResult.INFO,
			ValidateTokenEndpointAuthSigningAlg.class, ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(ValidateDefaultMaxAge.class, ConditionResult.WARNING,"OIDCR-2");

		skipIfElementMissing("client", "require_auth_time", ConditionResult.INFO,
			ValidateRequireAuthTime.class, ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "default_acr_values", ConditionResult.INFO,
			ValidateDefaultAcrValues.class, ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "initiate_login_uri", ConditionResult.INFO,
			ValidateInitiateLoginUri.class, ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "request_uris", ConditionResult.INFO,
			ValidateRequestUris.class, ConditionResult.FAILURE, "OIDCR-2");
	}

	protected void processAndValidateClientJwks() {
		JsonObject client = env.getObject("client");
		if(client.has("jwks")) {
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			validateClientJwks();
		}
	}

	protected void validateClientJwks() {
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, ConditionResult.FAILURE, "RFC7517-9.2");
	}

	protected void fetchAndProcessRequestUri() {
		callAndStopOnFailure(FetchRequestUriAndExtractRequestObject.class, "OIDCC-6.2");
		callAndStopOnFailure(EnsureRequestUriIsHttps.class, "OIDCC-6.2");
	}

	protected void extractAuthorizationEndpointRequestParameters() {
		if(clientRequestType == VPID2VerifierRequestMethod.REQUEST_URI_SIGNED) {
			fetchAndProcessRequestUri();
//		} else if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {
//			callAndStopOnFailure(ExtractRequestObject.class, "OIDCC-6.1");
//		} else {
//			//handle plain http request case
//			callAndStopOnFailure(EnsureRequestDoesNotContainRequestObject.class, "OIDCC-6.1");
		}

		if(clientRequestType == VPID2VerifierRequestMethod.REQUEST_URI_SIGNED) {
			validateRequestObject();
			callAndStopOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class);
			skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class,
										ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class, "OIDCC-6.1", "OIDCC-6.2");

		// FIXME extract presentation definition

		extractNonceFromAuthorizationEndpointRequestParameters();

		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CODE_CHALLENGE, ConditionResult.INFO, EnsureAuthorizationRequestContainsPkceCodeChallenge.class, ConditionResult.FAILURE, "RFC7636-4.3");
	}

	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-ID2-5.2");
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
		callAndContinueOnFailure(ValidateClientIdScheme.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationRequest.class, ConditionResult.WARNING);

		callAndContinueOnFailure(EnsureMatchingClientId.class, ConditionResult.FAILURE,"OIDCC-3.1.2.1");

		// check redirect uri not present

		callAndContinueOnFailure(EnsureValidResponseUriForAuthorizationEndpointRequest.class, ConditionResult.FAILURE,"OID4VP-ID2-6.2");

		// FIXME: validate rest of request
		// FIXME: validate client_metadata
		callAndContinueOnFailure(CheckForUnexpectedParametersInVpClientMetadata.class, ConditionResult.WARNING);


		endTestIfRequiredAuthorizationRequestParametersAreMissing();
	}

	protected void validateRequestObject() {
		skipIfElementMissing("authorization_request_object", "claims.exp", ConditionResult.INFO,
			OIDCCValidateRequestObjectExp.class, ConditionResult.FAILURE, "RFC7519-4.1.4");
		callAndContinueOnFailure(ValidateRequestObjectIat.class, ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(ValidateRequestObjectMaxAge.class, ConditionResult.FAILURE, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.WARNING, "JAR-10.8");

		//https://openid.net/specs/openid-connect-core-1_0.html#RequestObject
		// The Request Object MAY be signed or unsigned (plaintext).
		// When it is plaintext, this is indicated by use of the none algorithm [JWA] in the JOSE Header.
		// If signed, the Request Object SHOULD contain the Claims iss (issuer) and aud (audience) as members.
		// The iss value SHOULD be the Client ID of the RP, unless it was signed by a different party than the RP.
		// The aud value SHOULD be or include the OP's Issuer Identifier URL.
		// FIXME: https://github.com/openid/OpenID4VP/issues/299
		//callAndContinueOnFailure(ValidateRequestObjectIss.class, ConditionResult.WARNING, "OIDCC-6.1");

		// FIXME needs to allow self-issued.me
		//callAndContinueOnFailure(ValidateRequestObjectAud.class, ConditionResult.WARNING, "OIDCC-6.1");

		// FIXME probably need to somehow validate the x5c header is trusted/valid for the client
		callAndContinueOnFailure(ValidateRequestObjectSignatureAgainstX5cHeader.class, ConditionResult.FAILURE, "OID4VP-ID3-5.10.4");
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

		// FIXME not sure why this might be missing? the unexpected claims stuff should be on the auth parameters, not the request object ones
//		skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
//			CheckForUnexpectedClaimsInRequestObject.class, ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInClaimsParameter.class, ConditionResult.WARNING, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckForUnexpectedOpenIdClaims.class, ConditionResult.WARNING, "OIDCC-5.1", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.3", "BrazilOB-5.2.2.4", "OBSP-3.4");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckRequestObjectClaimsParameterValues.class, ConditionResult.FAILURE, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckRequestObjectClaimsParameterMemberValues.class, ConditionResult.FAILURE, "OIDCC-5.5.1");

		switch (getVariant(VPID2VerifierCredentialFormat.class)) {
			case SD_JWT_VC -> {
				callAndStopOnFailure(CreateVPID2SdJwtVpToken.class);
				callAndStopOnFailure(CreateVPID2SdJwtPresentationSubmission.class);
			}
			case ISO_MDL -> {
				callAndStopOnFailure(CreateMDocGeneratedNonce.class);
				callAndStopOnFailure(CreateWalletIsoMdlAnnexBSessionTranscript.class);
				callAndStopOnFailure(CreateMdocCredential.class);
				callAndStopOnFailure(CreateIsoMdocPresentationSubmission.class);
			}
		}

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddPeVpTokenToAuthorizationEndpointResponseParams.class, "OIDVP-FIXME");
		callAndStopOnFailure(AddPresentationSubmissionToAuthorizationEndpointResponseParams.class, "OIDVP-FIXME");

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

	protected void sendAuthorizationResponseToResponseUri() {
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(BuildUnsignedRequestToDirectPostEndpoint.class);
				break;
			case DIRECT_POST_JWT:
				callAndStopOnFailure(EncryptVPResponse.class);
				break;
		}
		callAndStopOnFailure(CallDirectPostEndpoint.class);

		call(exec().mapKey("endpoint_response", "direct_post_response"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE, "OID4VP-ID3-8.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "OID4VP-ID3-8.2");
		callAndContinueOnFailure(ValidateDirectPostResponse.class, ConditionResult.WARNING, "OID4VP-ID3-8.2");
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
