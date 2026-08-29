package net.openid.conformance.vp1finalverifier;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AbstractCreateStatusListReference;
import net.openid.conformance.condition.as.AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CheckDCQLQueryCredentialFormatMatchesTestConfiguration;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckNoClientIdSchemeParameter;
import net.openid.conformance.condition.as.CheckNoPresentationDefinitionInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckNoRedirectUriInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckNoScopeParameter;
import net.openid.conformance.condition.as.CheckNoTransactionDataInVpAuthorizationRequest;
import net.openid.conformance.condition.as.CheckRequestUriMethodParameter;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateMDocGeneratedNonce;
import net.openid.conformance.condition.as.CreateMdocCredential;
import net.openid.conformance.condition.as.CreateRevokedIdentifierListReference;
import net.openid.conformance.condition.as.CreateSdJwtKbCredential;
import net.openid.conformance.condition.as.CreateValidStatusListReference;
import net.openid.conformance.condition.as.EnsureMatchedRicalEntryHasNoTrustConstraints;
import net.openid.conformance.condition.as.EnsureVerifierFetchedIdentifierList;
import net.openid.conformance.condition.as.EnsureVerifierFetchedStatusList;
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
import net.openid.conformance.condition.as.ExtractVerifierInfoFromAuthorizationRequest;
import net.openid.conformance.condition.as.FetchRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.as.OID4VPSetClientIdToIncludeClientIdScheme;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SetRequestUriParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.VP1FinalCheckEncryptionKeyNotReused;
import net.openid.conformance.condition.as.VP1FinalCheckForKeyIdInClientMetadataJWKs;
import net.openid.conformance.condition.as.VP1FinalCheckForUnexpectedParametersInVpClientMetadata;
import net.openid.conformance.condition.as.VP1FinalEncryptVPResponse;
import net.openid.conformance.condition.as.VP1FinalGenerateCwtStatusListToken;
import net.openid.conformance.condition.as.VP1FinalGenerateIdentifierListToken;
import net.openid.conformance.condition.as.VP1FinalGenerateJwtStatusListToken;
import net.openid.conformance.condition.as.VP1FinalValidateClientMetadataJwksForEncryptedResponse;
import net.openid.conformance.condition.as.VP1FinalValidateVpFormatsSupportedInClientMetadata;
import net.openid.conformance.condition.as.VP1FinalEnsureDirectPostResponseHasRedirectUriForHaip;
import net.openid.conformance.condition.as.ValidateDirectPostResponse;
import net.openid.conformance.condition.as.ValidateDirectPostResponseRedirectUriWhenPresent;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRequestObjectAudForVP;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectIssIfPresent;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignatureAgainstX5cHeader;
import net.openid.conformance.condition.as.ValidateRequestObjectX5cChainAgainstRical;
import net.openid.conformance.condition.as.ValidateRequestObjectTypIsOAuthQauthReqJwt;
import net.openid.conformance.condition.as.ValidateResponseMode;
import net.openid.conformance.condition.as.ValidateVpClientMetadataEncryptionForHaip;
import net.openid.conformance.condition.as.WarnIfRequestUriMethodInRequestObject;
import net.openid.conformance.condition.client.BuildUnsignedRequestToDirectPostEndpoint;
import net.openid.conformance.condition.client.CallDirectPostEndpoint;
import net.openid.conformance.condition.client.CheckForNonSelectivelyDisclosableClaimsInDcqlQuery;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInDcqlQuery;
import net.openid.conformance.condition.client.CheckForUnreferencedClaimsInDcqlQuery;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInVerifierInfo;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted;
import net.openid.conformance.condition.client.CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptUnencrypted;
import net.openid.conformance.condition.client.EnsureClientRequestObjectTrustAnchorConfigured;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.RegisterClientRequestObjectTrustAnchor;
import net.openid.conformance.condition.client.ValidateDCQLQuery;
import net.openid.conformance.condition.client.ValidateOwnMdocSigningChainAgainstVical;
import net.openid.conformance.condition.client.ValidateVerifierInfo;
import net.openid.conformance.condition.common.ExpectVerifierSuccessfulVerificationPage;
import net.openid.conformance.sequence.ValidateJwksSequence;
import net.openid.conformance.sequence.client.SetupRicalFromConfiguration;
import net.openid.conformance.sequence.client.SetupVicalFromConfiguration;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.HtmlUtils;


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
@VariantConfigurationFields(parameter = VPProfile.class, value = "haip", configurationFields = {
	"client.request_object_trust_anchor_pem"
})
@ConfigurationFields({
	"client.rical",
	"client.rical_url"
})
@VariantConfigurationFields(parameter = VP1FinalVerifierCredentialFormat.class, value = "iso_mdl", configurationFields = {
	"credential.vical",
	"credential.vical_url"
})
@VariantNotApplicableWhen(
	parameter = VP1FinalVerifierResponseMode.class,
	values = {"direct_post"},  // unencrypted mode not applicable for HAIP
	whenParameter = VPProfile.class,
	hasValues = "haip"
)
// Per OID4VP 1.0 Final § 5.9.3-3.1.1, requests using the redirect_uri Client Identifier Prefix
// cannot be signed — there is no method for the Wallet to obtain a trusted key for verification.
@VariantNotApplicableWhen(
	parameter = VP1FinalVerifierRequestMethod.class,
	values = {"request_uri_signed"},
	whenParameter = VP1FinalVerifierClientIdPrefix.class,
	hasValues = "redirect_uri"
)
// Per OID4VP 1.0 Final § 5.9.3-3.5.1 (x509_san_dns) and § 5.9.3-3.6.1 (x509_hash), the request
// MUST be signed and the prefix references the certificate in the x5c JOSE header of the signed
// request object — there is no signed request object in the url_query method, so neither prefix
// is applicable to it.
@VariantNotApplicableWhen(
	parameter = VP1FinalVerifierRequestMethod.class,
	values = {"url_query"},
	whenParameter = VP1FinalVerifierClientIdPrefix.class,
	hasValues = {"x509_hash", "x509_san_dns"}
)
public abstract class AbstractVP1FinalVerifierTest extends AbstractTestModule {

	/**
	 * Path (under this test instance's base url) of the page served by
	 * {@link #handleVerificationEvidenceRequest()}.
	 */
	protected static final String VERIFICATION_EVIDENCE_PATH = "verification-evidence";

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

		callAndStopOnFailure(RegisterClientRequestObjectTrustAnchor.class);
		// register and validate the optionally configured RICAL used as the trust source for
		// the verifier's request object signing chain (superseding the single trust anchor)
		call(sequence(SetupRicalFromConfiguration.class));
		if (getVariant(VP1FinalVerifierCredentialFormat.class) == VP1FinalVerifierCredentialFormat.ISO_MDL) {
			// register and validate the optionally configured VICAL, then pre-flight check
			// that the suite's own mdoc signing chain is covered by it - a verifier trusting
			// the VICAL is expected to reject the presented mdocs otherwise. A WARNING as
			// this reports on the suite's VICAL registration, not on the verifier under test.
			call(sequence(SetupVicalFromConfiguration.class));
			call(condition(ValidateOwnMdocSigningChainAgainstVical.class)
				.skipIfObjectsMissing("vical")
				.onSkip(ConditionResult.INFO)
				.onFail(ConditionResult.WARNING)
				.dontStopOnFailure()
				.requirements("ISO18013-5-C.1.7.1"));
		}
		if (getVariant(VPProfile.class) == VPProfile.HAIP) {
			callAndContinueOnFailure(EnsureClientRequestObjectTrustAnchorConfigured.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.9.3");
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		getBrowser().requestUriInput(env.getString("authorization_endpoint"),
			"Paste the openid4vp:// authorization request produced by the verifier under test; its query string will be delivered to this test's authorization endpoint.");
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {
		if (AbstractCreateStatusListReference.STATUS_LIST_PATH.equals(path)) {
			// served without moving the test to RUNNING: that takes the test lock, which is
			// held for the whole of the authorization endpoint handler - including the POST of
			// the authorization response to the verifier's response_uri. A verifier that checks
			// the status list before it responds to that POST would therefore deadlock against
			// itself. The token is generated when the credential is created and stored in the
			// environment, so this handler only has to read it, which needs no lock (the
			// environment is backed by a concurrent map).
			return handleStatusListRequest();
		}
		if (CreateRevokedIdentifierListReference.IDENTIFIER_LIST_PATH.equals(path)) {
			// lock-free for the same reason as the status list above
			return handleIdentifierListRequest();
		}
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
	private Object handleStatusListRequest() {
		boolean isMdoc = getVariant(VP1FinalVerifierCredentialFormat.class)
			== VP1FinalVerifierCredentialFormat.ISO_MDL;
		String contentType = isMdoc
			? VP1FinalGenerateCwtStatusListToken.STATUS_LIST_CWT_CONTENT_TYPE
			: VP1FinalGenerateJwtStatusListToken.STATUS_LIST_JWT_CONTENT_TYPE;
		String token = env.getString(isMdoc
			? VP1FinalGenerateCwtStatusListToken.ENV_KEY : VP1FinalGenerateJwtStatusListToken.ENV_KEY);

		if (token == null) {
			eventLog.log(getName(), "The verifier requested the status list before the presentation "
				+ "was sent, so there is no status list token to serve yet.");
			return ResponseEntity.notFound().build();
		}

		env.putString(EnsureVerifierFetchedStatusList.FETCHED_ENV_KEY, "true");
		eventLog.log(getName(), "The verifier fetched the status list the presented credential references.");

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE, contentType)
			.body(isMdoc ? Base64.getDecoder().decode(token) : token);
	}

	/**
	 * Serves the identifier list (ISO/IEC 18013-5 12.3.6.4) an mdoc's MSO references, for the
	 * tests that use that revocation mechanism instead of the status list one. Lock-free, exactly
	 * as {@link #handleStatusListRequest()} is and for the same deadlock reason.
	 */
	private Object handleIdentifierListRequest() {
		String token = env.getString(VP1FinalGenerateIdentifierListToken.ENV_KEY);

		if (token == null) {
			eventLog.log(getName(), "The verifier requested the identifier list before the "
				+ "presentation was sent, so there is no identifier list to serve yet.");
			return ResponseEntity.notFound().build();
		}

		env.putString(EnsureVerifierFetchedIdentifierList.FETCHED_ENV_KEY, "true");
		eventLog.log(getName(), "The verifier fetched the identifier list the presented credential "
			+ "references.");

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_TYPE,
				VP1FinalGenerateIdentifierListToken.IDENTIFIER_LIST_CWT_CONTENT_TYPE)
			.body(Base64.getDecoder().decode(token));
	}

	@Override
	public void fireTestFinished() {
		// A verifier that never fetched the referenced status list cannot have checked the
		// credential's status. HAIP requires verifiers to support validating a credential's
		// status information (HAIP 7-2.2.2.2), so under HAIP a missing fetch is a failure;
		// outside HAIP checking is the verifier's policy choice and only warned about. Skipped
		// when no status list reference was created (e.g. the configuration-skipped path).
		call(condition(EnsureVerifierFetchedStatusList.class)
			.skipIfObjectsMissing(AbstractCreateStatusListReference.ENV_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(getVariant(VPProfile.class) == VPProfile.HAIP
				? ConditionResult.FAILURE : ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("HAIP-7-2.2.2.2"));
		// the identifier list mechanism counterpart of the check above; ISO/IEC 18013-5 (unlike
		// HAIP for the status list) states no requirement on the verifier fetching it, so this is
		// a warning under every profile
		call(condition(EnsureVerifierFetchedIdentifierList.class)
			.skipIfObjectsMissing(CreateRevokedIdentifierListReference.ENV_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("ISO18013-5-12.3.6.2"));
		super.fireTestFinished();
	}

	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse){

		if (path.equals("authorize")) {
			receivedAuthorizationRequest = true;
			return handleAuthorizationEndpointRequest(requestId);
		} else if (path.equals(VERIFICATION_EVIDENCE_PATH)) {
			return handleVerificationEvidenceRequest();
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
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "verifier_info", ConditionResult.INFO,
			ExtractVerifierInfoFromAuthorizationRequest.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "verifier_info", ConditionResult.INFO,
			ValidateVerifierInfo.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5.1");
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "verifier_info", ConditionResult.INFO,
			CheckForUnexpectedParametersInVerifierInfo.class, ConditionResult.WARNING, "OID4VP-1FINAL-5.1");

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
				call(new ValidateJwksSequence(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.jwks", "client_metadata", "OID4VP-1FINAL-5.1"));
				if (getVariant(VPProfile.class) == VPProfile.HAIP) {
					callAndContinueOnFailure(ValidateVpClientMetadataEncryptionForHaip.class, ConditionResult.FAILURE, "HAIP-5-5", "OID4VP-1FINAL-8.3");
				}
				// HAIP requires an ephemeral encryption key specific to each Authorization Request; reuse is a
				// FAILURE under HAIP and a WARNING otherwise (base OID4VP §8.3 does not clearly mandate it).
				ConditionResult reuseSeverity = (getVariant(VPProfile.class) == VPProfile.HAIP)
					? ConditionResult.FAILURE : ConditionResult.WARNING;
				callAndContinueOnFailure(VP1FinalCheckEncryptionKeyNotReused.class, reuseSeverity, "HAIP-5-5", "OID4VP-1FINAL-8.3");
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

		// Skipped unless a RICAL is configured. The verifier under test owns its reader
		// certificate, so a chain that does not validate against the configured reader trust
		// list is a FAILURE.
		call(condition(ValidateRequestObjectX5cChainAgainstRical.class)
			.skipIfObjectsMissing("rical")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.dontStopOnFailure()
			.requirements("ISO18013-5-F.3.2.6"));
		// trust constraints are ecosystem-defined with no concrete types in the spec, so the
		// suite cannot evaluate them - a WARNING, to be raised per-profile once an ecosystem
		// defines machine-checkable constraint semantics
		call(condition(EnsureMatchedRicalEntryHasNoTrustConstraints.class)
			.skipIfObjectsMissing("rical")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure()
			.requirements("ISO18013-5-F.3.2.3"));
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
		callAndContinueOnFailure(CheckForNonSelectivelyDisclosableClaimsInDcqlQuery.class, ConditionResult.WARNING, "SDJWTVC-3.2.2.2");
		callAndContinueOnFailure(CheckForUnreferencedClaimsInDcqlQuery.class, ConditionResult.WARNING, "OID4VP-1FINAL-6.4.1");
		// Test harness check: ensures verifier requests the credential format matching the test configuration
		callAndContinueOnFailure(CheckDCQLQueryCredentialFormatMatchesTestConfiguration.class, ConditionResult.FAILURE);

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		createCredential();

		callAndStopOnFailure(AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams.class, "OID4VP-1FINAL-8.1");

		customizeAuthorizationEndpointResponseParams();

		sendAuthorizationResponseToResponseUri();

		Object viewToReturn;

		// Spec-level validity of redirect_uri (must be a non-empty string when present) is
		// enforced by ValidateDirectPostResponseRedirectUriWhenPresent at FAILURE; the read
		// here is intentionally defensive so a wallet that sent garbage still surfaces the
		// proper condition failure rather than crashing the test module.
		JsonElement redirectEl = env.getElementFromObject("direct_post_response", "body_json.redirect_uri");
		String redirectTo = null;
		if (redirectEl != null && redirectEl.isJsonPrimitive() && redirectEl.getAsJsonPrimitive().isString()) {
			String value = OIDFJSON.getString(redirectEl);
			if (!value.isEmpty()) {
				redirectTo = value;
			}
		}
		if (redirectTo != null) {
			viewToReturn = new RedirectView(redirectTo, false, false, false);
		} else {
			viewToReturn = new ModelAndView("resultCaptured",
				ImmutableMap.of(
					"returnUrl", "/log-detail.html?log=" + getId()
				));
		}

		if (directPostResponseWas2xx()) {
			// The verifier accepted the response at the response_uri. Whether it actually
			// verified the VP Token is not observable over HTTP (deferred verification is
			// permitted), so require a screenshot of the verification result. Leaving
			// testFinished false makes handleHttp() move the test to WAITING once this
			// handler returns; waitForPlaceholders() finishes the test (result REVIEW)
			// when the screenshot has been uploaded.
			createScreenshotPlaceholder();
			fillScreenshotPlaceholderViaBrowserAutomationIfConfigured();
			waitForPlaceholders();
		} else {
			// The direct post did not succeed: for negative tests a 4xx is the immediate
			// pass; for positive tests a condition failure has already been recorded.
			testFinished = true;
		}

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return viewToReturn;
	}

	/**
	 * Creates the credential the emulated wallet presents, in the format the selected variant
	 * asks for. Tests that need to set something up before the credential exists (e.g. the status
	 * list reference it carries) override this and call super.
	 */
	protected void createCredential() {
		// must run before the credential is created; the credential carries the reference
		createStatusListReference();
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
		// generate the status list token now so it is ready to serve however quickly the
		// verifier fetches it - see handleStatusListRequest
		switch (getVariant(VP1FinalVerifierCredentialFormat.class)) {
			case SD_JWT_VC ->
				callAndStopOnFailure(VP1FinalGenerateJwtStatusListToken.class, "OTSL-5.1");
			case ISO_MDL ->
				callAndStopOnFailure(VP1FinalGenerateCwtStatusListToken.class, "OTSL-5.2",
					"ISO18013-5-12.3.6.3");
		}
	}

	/**
	 * Allocates the status list reference the presented credential will carry. The happy flows
	 * reference an index the served status list marks as valid, so that verifiers exercise the
	 * status fetch on a good credential; negative tests override this to allocate a revoked one.
	 */
	protected void createStatusListReference() {
		callAndStopOnFailure(CreateValidStatusListReference.class, "OTSL-6.2", "ISO18013-5-12.3.6.2");
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
		callAndContinueOnFailure(ValidateDirectPostResponseRedirectUriWhenPresent.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
		if (getVariant(VPProfile.class) == VPProfile.HAIP) {
			callAndContinueOnFailure(VP1FinalEnsureDirectPostResponseHasRedirectUriForHaip.class, ConditionResult.FAILURE, "HAIP-5.1");
		}
	}

	protected boolean directPostResponseWas2xx() {
		Integer status = env.getInteger("direct_post_response", "status");
		return status != null && status >= 200 && status <= 299;
	}

	/**
	 * Creates the browser-interaction placeholder the tester fills with a screenshot of the
	 * verifier's verification result. OID4VP 1.0 Final does not require VP Token verification
	 * to complete before the response_uri response, so the screenshot (plus verifier logs in
	 * the certification package) is the evidence that verification actually happened.
	 * Negative tests override this to expect a rejection page instead.
	 */
	protected void createScreenshotPlaceholder() {
		callAndStopOnFailure(ExpectVerifierSuccessfulVerificationPage.class, "OID4VP-1FINAL-8.2");
	}

	/**
	 * If the test configuration contains a 'browser' automation entry matching this test's
	 * verification-evidence page (the CI configs do), send the scripted browser there with the
	 * screenshot placeholder attached, so an 'update-image-placeholder' task can fill the
	 * placeholder without human interaction. Without matching automation (normal certification
	 * runs) this does nothing and the tester uploads the screenshot manually.
	 */
	protected void fillScreenshotPlaceholderViaBrowserAutomationIfConfigured() {
		String evidenceUrl = env.getString("base_url") + "/" + VERIFICATION_EVIDENCE_PATH;
		if (browser.urlMatchesBrowserAutomation(evidenceUrl)) {
			browser.goToUrl(evidenceUrl, env.getString("verifier_verification_result_screenshot"));
		}
	}

	/**
	 * Serves a plain HTML snapshot of the response_uri exchange for the scripted browser to
	 * capture into the screenshot placeholder; in automated runs there is no verifier UI a
	 * human could take a real screenshot of.
	 */
	protected Object handleVerificationEvidenceRequest() {
		Integer status = env.getInteger("direct_post_response", "status");
		JsonElement bodyJson = env.getElementFromObject("direct_post_response", "body_json");
		String html = "<!DOCTYPE html><html><head><title>Verification evidence</title></head><body>"
			+ "<h1>Deferred verification evidence</h1>"
			+ "<p>Automated stand-in for the verifier's verification-result screenshot: the verifier "
			+ "accepted the authorization response at the response_uri with HTTP status "
			+ HtmlUtils.htmlEscape(String.valueOf(status)) + ".</p>"
			+ (bodyJson == null ? "" : "<pre>" + HtmlUtils.htmlEscape(bodyJson.toString()) + "</pre>")
			+ "</body></html>";
		return ResponseEntity.ok()
			.contentType(MediaType.TEXT_HTML)
			.body(html);
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
