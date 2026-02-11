package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInBindingJwt;
import net.openid.conformance.condition.as.CheckForUnexpectedParametersInBindingJwtHeader;
import net.openid.conformance.condition.as.OID4VPSetClientIdToIncludeClientIdScheme;
import net.openid.conformance.condition.client.AddClientIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddDcqlToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddExpectedOriginsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddRedirectUriToDirectPostResponse;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddSelfIssuedMeV2AudToRequestObject;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddVP1FinalEncryptionParametersToClientMetadata;
import net.openid.conformance.condition.client.AddVP1FinalIsoMdocClientMetadataToAuthorizationRequest;
import net.openid.conformance.condition.client.AddVP1FinalSdJwtClientMetadataToAuthorizationRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.BuildVP1FinalBrowserDCAPIRequestSigned;
import net.openid.conformance.condition.client.BuildVP1FinalBrowserDCAPIRequestUnsigned;
import net.openid.conformance.condition.client.CheckAudInBindingJwt;
import net.openid.conformance.condition.client.CheckAudInBindingJwtDcApi;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsGet;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInVpAuthorizationResponse;
import net.openid.conformance.condition.client.CheckIatInBindingJwt;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfClientIdInX509CertSanDns;
import net.openid.conformance.condition.client.CheckNoPresentationSubmissionParameter;
import net.openid.conformance.condition.client.CheckNonceInBindingJwt;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CheckTypInBindingJwt;
import net.openid.conformance.condition.client.CheckUrlFragmentContainsCodeVerifier;
import net.openid.conformance.condition.client.CheckUrlQueryIsEmpty;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateClientEncryptionKeyIfMissing;
import net.openid.conformance.condition.client.CreateDirectPostResponseUri;
import net.openid.conformance.condition.client.CreateEmptyAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateEmptyDirectPostResponse;
import net.openid.conformance.condition.client.CreateRandomCodeVerifier;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript;
import net.openid.conformance.condition.client.CreateVP1FinalWalletIsoMdocRedirectSessionTranscript;
import net.openid.conformance.condition.client.DecryptResponse;
import net.openid.conformance.condition.client.EnsureIncomingRequestContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.EnsureIncomingUrlQueryIsEmpty;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromFormBody;
import net.openid.conformance.condition.client.ExtractBrowserApiAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractVP1FinalBrowserApiResponse;
import net.openid.conformance.condition.client.ExtractVP1FinalVpTokenDCQL;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.ParseCredentialAsMdoc;
import net.openid.conformance.condition.client.ParseCredentialAsSdJwtKb;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseMode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToVpToken;
import net.openid.conformance.condition.client.SetClientIdToResponseUri;
import net.openid.conformance.condition.client.SetClientIdToResponseUriHostnameIfUnset;
import net.openid.conformance.condition.client.SetClientIdToWebOrigin;
import net.openid.conformance.condition.client.SetClientIdToX509Hash;
import net.openid.conformance.condition.client.SetWebOrigin;
import net.openid.conformance.condition.client.SignRequestObjectIncludeTypHeader;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeader;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderIfAvailable;
import net.openid.conformance.condition.client.ValidateAuthResponseContainsOnlyResponse;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateCredentialCnfJwkIsPublicKey;
import net.openid.conformance.condition.client.ValidateCredentialIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateJWEBodyDoesNotIncludeIssExpAud;
import net.openid.conformance.condition.client.ValidateJWEHeaderCtyJson;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.condition.client.ValidateSdJwtKeyBindingSignature;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CreateRandomBrowserApiSubmitUrl;
import net.openid.conformance.condition.common.CreateRandomRequestUriWithoutFragment;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@VariantParameters({
	VPProfile.class,
	VP1FinalWalletCredentialFormat.class,
	VP1FinalWalletClientIdPrefix.class,
	VP1FinalWalletResponseMode.class,
	VP1FinalWalletRequestMethod.class
})
@VariantConfigurationFields(parameter = VP1FinalWalletClientIdPrefix.class, value = "decentralized_identifier", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = VP1FinalWalletClientIdPrefix.class, value = "pre_registered", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = VP1FinalWalletClientIdPrefix.class, value = "x509_san_dns", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = VP1FinalWalletResponseMode.class, value = "direct_post", configurationFields = {
	"server.authorization_endpoint"
})
@VariantConfigurationFields(parameter = VP1FinalWalletResponseMode.class, value = "direct_post.jwt", configurationFields = {
	"server.authorization_endpoint",
	"client.authorization_encrypted_response_alg",
	"client.authorization_encrypted_response_enc"
})
@VariantConfigurationFields(parameter = VP1FinalWalletResponseMode.class, value = "dc_api.jwt", configurationFields = {
	"client.authorization_encrypted_response_alg",
	"client.authorization_encrypted_response_enc"
})
@VariantNotApplicableWhen(
	parameter = VP1FinalWalletResponseMode.class,
	values = {"direct_post", "dc_api"},  // all values
	whenParameter = VPProfile.class,
	hasValues = "haip"
)
public abstract class AbstractVP1FinalWalletTest extends AbstractRedirectServerTestModule {
	protected enum TestState {
		INITIAL,
		REQUEST_SENT, // if there's a request uri, this state is only used after it has been called
		RESPONSE_RECEIVED,
	}

	protected VP1FinalWalletResponseMode responseMode;
	protected VP1FinalWalletRequestMethod requestMethod;
	protected VP1FinalWalletCredentialFormat credentialFormat;
	protected VP1FinalWalletClientIdPrefix clientIdPrefix;
	protected TestState testState = TestState.INITIAL;

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
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
		abortIfRedirectFragmentNotReceived = true;

		responseMode = getVariant(VP1FinalWalletResponseMode.class);
		env.putString("response_mode", responseMode.toString());
		credentialFormat = getVariant(VP1FinalWalletCredentialFormat.class);
		requestMethod = getVariant(VP1FinalWalletRequestMethod.class);
		clientIdPrefix = getVariant(VP1FinalWalletClientIdPrefix.class);
		env.putString("client_id_scheme", clientIdPrefix.toString());

		// As per ISO 18013-7 B.5.3 "Nonces shall have a minimum length of 16 bytes"
		env.putInteger("requested_nonce_length", 16);

		switch (responseMode) {
			case DIRECT_POST:
			case DIRECT_POST_JWT:
				callAndStopOnFailure(CreateDirectPostResponseUri.class);
				break;
			case DC_API_JWT:
			case DC_API:
				callAndStopOnFailure(SetWebOrigin.class, "OID4VP-1FINAL-2");
				break;
		}

		switch (clientIdPrefix) {
			case DECENTRALIZED_IDENTIFIER:
			case PRE_REGISTERED:
				// client id has been set already in config
				break;
			case WEB_ORIGIN:
				callAndStopOnFailure(SetClientIdToWebOrigin.class);
				break;
			case REDIRECT_URI:
				callAndStopOnFailure(SetClientIdToResponseUri.class);
				break;
			case X509_SAN_DNS:
				callAndStopOnFailure(SetClientIdToResponseUriHostnameIfUnset.class);
				break;
			case X509_HASH:
				callAndStopOnFailure(SetClientIdToX509Hash.class);
				break;
		}
		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("response_uri");

		callAndStopOnFailure(GetStaticServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
//		callAndStopOnFailure(CheckServerConfiguration.class); // FIXME doesn't like the openid4vp:// url being set as authorization endpoint url

		// Set up the client configuration
		configureClient();

		if (credentialFormat == VP1FinalWalletCredentialFormat.ISO_MDL) {
			// ISO spec always creates a redirect returned from response_uri
			callAndStopOnFailure(CreateRedirectUri.class);
		}

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		callAndStopOnFailure(CreateRandomRequestUriWithoutFragment.class, "JAR-5.2");
		browser.setShowQrCodes(true);
	}

	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndStopOnFailure(OID4VPSetClientIdToIncludeClientIdScheme.class);

		configureStaticClient();

		exposeEnvString("client_id");

		completeClientConfiguration();
	}

	protected void configureStaticClient() {
		JsonElement clientJwksEl = env.getElementFromObject("client", "jwks");
		boolean jwksRequired = false;
		boolean encryptionKeyRequired = false;
		// keys are needed for signed requests or encrypted responses
		switch (requestMethod) {
//				case URL_QUERY:
			case REQUEST_URI_UNSIGNED:
				break;
			case REQUEST_URI_SIGNED:
				jwksRequired = true;
				break;
		}
		switch (responseMode) {
			case DIRECT_POST:
			case DC_API:
				break;
			case DIRECT_POST_JWT:
			case DC_API_JWT:
				// assume response is encrypted so a key is required
				jwksRequired = true;
				encryptionKeyRequired = true;
				break;
		}

		if (clientJwksEl == null && !jwksRequired) {
			return;
		}

		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		if (encryptionKeyRequired) {
			callAndStopOnFailure(CreateClientEncryptionKeyIfMissing.class);
		}
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
	}

	protected void completeClientConfiguration() {
		if (clientIdPrefix == VP1FinalWalletClientIdPrefix.X509_SAN_DNS) {
			callAndContinueOnFailure(CheckIfClientIdInX509CertSanDns.class, ConditionResult.FAILURE);
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performAuthorizationFlow();
	}

	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to wallet");
		createAuthorizationRequest();
		createAuthorizationRedirect();
		switch (responseMode) {
			case DC_API:
			case DC_API_JWT:
				callAndStopOnFailure(CreateRandomBrowserApiSubmitUrl.class);
				String submitUrl = env.getString("browser_api_submit", "fullUrl");



				switch (requestMethod) {
					case REQUEST_URI_UNSIGNED -> {
						callAndStopOnFailure(BuildVP1FinalBrowserDCAPIRequestUnsigned.class);
					}
					case REQUEST_URI_SIGNED -> {
						callAndStopOnFailure(BuildVP1FinalBrowserDCAPIRequestSigned.class);
					}
				}
				JsonObject request = env.getObject("browser_api_request");

				eventLog.log(getName(), args("msg", "Calling browser API",
					"request", request,
					"http", "api"));

				browser.requestCredential(request, submitUrl);
				setStatus(Status.WAITING);

				eventLog.log(getName(), "The wallet should be opened using the Browser API button, and should then parse the request and return the results via the API");
				break;
			case DIRECT_POST_JWT:
			case DIRECT_POST:
				performRedirect();
				eventLog.log(getName(), "The wallet should be opened via the QR code / proceed with test button, and should then fetch the request_uri");
				break;
		}
		eventLog.endBlock();
	}
	// FIXME when waiting for implicit submit set a timeout, to make it clearer when people are treating the redirect_url from direct_post endpoint as a http endpoint
	// FIXME send parameters in openid4vp:// url in a different order
	// FIXME test without use: enc in client_metadata

	public static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {
		private VP1FinalWalletRequestMethod requestMethod;
		private VP1FinalWalletResponseMode responseMode;
		private VP1FinalWalletCredentialFormat credentialFormat;

		public CreateAuthorizationRequestSteps(VP1FinalWalletRequestMethod requestMethod, VP1FinalWalletResponseMode responseMode, VP1FinalWalletCredentialFormat credentialFormat) {
			this.requestMethod = requestMethod;
			this.responseMode = responseMode;
			this.credentialFormat = credentialFormat;
		}

		@Override
		public void evaluate() {
			boolean browserApi = false;
			boolean browserUnsigned = false;
			switch (responseMode) {
				case DIRECT_POST:
				case DIRECT_POST_JWT:
					break;
				case DC_API:
				case DC_API_JWT:
					browserApi = true;
					switch (requestMethod) {
						case REQUEST_URI_UNSIGNED -> {
							browserUnsigned = true;
						}
						case REQUEST_URI_SIGNED -> {
						}
					}
					break;
			}

			callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
			if (!browserUnsigned) {
				// client id is not permitted in unsigned browser API requests
				callAndStopOnFailure(AddClientIdToAuthorizationEndpointRequest.class);
			}
			if (!browserApi) {
				// state & response_uri aren't permitted in browser API requests as per
				// https://openid.github.io/OpenID4VP/openid-4-verifiable-presentations-wg-draft.html#appendix-A.2
				callAndStopOnFailure(CreateRandomStateValue.class);
				call(exec().exposeEnvironmentString("state"));
				callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

				callAndStopOnFailure(AddResponseUriToAuthorizationEndpointRequest.class);
			}
			if (browserApi && !browserUnsigned) {
				callAndStopOnFailure(AddExpectedOriginsToAuthorizationEndpointRequest.class, "OID4VP-1FINAL-A.2");
			}

			callAndStopOnFailure(AddDcqlToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(CreateRandomNonceValue.class);
			call(exec().exposeEnvironmentString("nonce"));
			callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

			switch (credentialFormat) {
				case ISO_MDL:
					callAndStopOnFailure(AddVP1FinalIsoMdocClientMetadataToAuthorizationRequest.class);
					break;
				case SD_JWT_VC:
					callAndStopOnFailure(AddVP1FinalSdJwtClientMetadataToAuthorizationRequest.class);
					break;
			}
			switch (responseMode) {
				case DIRECT_POST:
				case DC_API:
					break;
				case DIRECT_POST_JWT:
				case DC_API_JWT:
					callAndStopOnFailure(AddVP1FinalEncryptionParametersToClientMetadata.class);
					break;
			}

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToVpToken.class);

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseMode.class);

		}
	}

	protected void createAuthorizationRequest() {
		call(createAuthorizationRequestSequence());
	}

	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = new CreateAuthorizationRequestSteps(requestMethod, responseMode, credentialFormat);

		return createAuthorizationRequestSteps;
	}

	protected Object handleDirectPost(String requestId) {

		setStatus(Status.RUNNING);

		switch (responseMode) {
			case DIRECT_POST:
			case DIRECT_POST_JWT:
				break;
			case DC_API:
			case DC_API_JWT:
				throw new TestFailureException(getId(), "Direct post response received but result was expected to be returned from the Browser API");
		}

		call(exec().startBlock("Direct post endpoint").mapKey("incoming_request", requestId));
		setStateToResponseReceived();
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsFormUrlEncoded.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureIncomingUrlQueryIsEmpty.class, ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromFormBody.class, ConditionResult.FAILURE);

		processReceivedResponse();

		// as per https://openid.bitbucket.io/connect/openid-4-verifiable-presentations-1_0.html#section-6.2
		callAndStopOnFailure(CreateEmptyDirectPostResponse.class, ConditionResult.FAILURE);
		switch (credentialFormat) {
			case ISO_MDL:
				// iso mdl spec requires that redirect uri is always returned, so we return it in all test modules
				// for other credential formats some test modules return a valid response without redirect uri
				populateDirectPostResponseWithRedirectUri();
				break;
			default:
				populateDirectPostResponse();
				break;
		}

		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.body(env.getObject("direct_post_response").toString());
	}

	// This is called for both the browser API response and the regular direct post response
	// The received response has been stored in original_authorization_endpoint_response,
	// and is unpacked (decrypted etc. if necessary) into authorization_endpoint_response
	private void processReceivedResponse() {
		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(ExtractAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				break;
			case DC_API:
				callAndStopOnFailure(ExtractBrowserApiAuthorizationEndpointResponse.class, ConditionResult.FAILURE);
				break;
			case DIRECT_POST_JWT:
			case DC_API_JWT:
				callAndStopOnFailure(ValidateAuthResponseContainsOnlyResponse.class, "OID4VP-1FINAL-7.3");
				// currently only supports encrypted-not-signed as used by mdl
				callAndStopOnFailure(DecryptResponse.class, "OID4VP-1FINAL-7.3");
				// FIXME: need to validate jwe header
				callAndContinueOnFailure(ValidateJWEHeaderCtyJson.class, ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateJWEBodyDoesNotIncludeIssExpAud.class, ConditionResult.FAILURE, "OID4VP-1FINAL-7.3");
				break;
		}

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(ExtractVP1FinalVpTokenDCQL.class, ConditionResult.FAILURE, "OID4VP-1FINAL-7.1");
		callAndContinueOnFailure(CheckNoPresentationSubmissionParameter.class, ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationResponse.class, ConditionResult.WARNING, "OID4VP-1FINAL-8");
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5");

		switch (credentialFormat) {
			case ISO_MDL:
				// mdoc
				callAndContinueOnFailure(ValidateCredentialIsUnpaddedBase64Url.class, ConditionResult.FAILURE);
				if (isBrowserApi()) {
					callAndStopOnFailure(CreateVP1FinalVerifierIsoMdocDCAPISessionTranscript.class, "OID4VP-1FINALA-B.2.6.2");
				} else {
					callAndStopOnFailure(CreateVP1FinalWalletIsoMdocRedirectSessionTranscript.class, "OID4VP-1FINALA-B.2.6.1");
				}
				callAndStopOnFailure(ParseCredentialAsMdoc.class);
				break;

			case SD_JWT_VC:
				callAndStopOnFailure(ParseCredentialAsSdJwtKb.class, ConditionResult.FAILURE);

				eventLog.startBlock(currentClientString() + "Verify credential JWT");
				// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must must not be selectively disclosed
				// FIXME check iss is a valid uri
				callAndContinueOnFailure(ValidateCredentialJWTIat.class, ConditionResult.FAILURE, "SDJWTVC-3.2.2.2-5.2");
				// FIXME nbf
				// FIXME exp
				callAndContinueOnFailure(ValidateCredentialCnfJwkIsPublicKey.class, ConditionResult.FAILURE, "SDJWT-4.1.2");
				// cnf is otherwise checked when holder binding is checked below
				// FIXME type
				// FIXME status

				eventLog.startBlock(currentClientString() + "Verify key binding JWT");

				callAndContinueOnFailure(ValidateSdJwtKeyBindingSignature.class, ConditionResult.FAILURE, "SDJWT-4.3");

				callAndContinueOnFailure(CheckTypInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-4.3");
				// alg is checked during signature validation
				callAndContinueOnFailure(CheckForUnexpectedParametersInBindingJwtHeader.class, ConditionResult.WARNING, "SDJWT-4.3");

				callAndContinueOnFailure(CheckIatInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-4.3");
				switch (responseMode) {
					case DIRECT_POST, DIRECT_POST_JWT -> {
						callAndContinueOnFailure(CheckAudInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-4.3", "OID4VP-1FINAL-B.4.5");
					}
					case DC_API, DC_API_JWT -> {
						callAndContinueOnFailure(CheckAudInBindingJwtDcApi.class, ConditionResult.FAILURE, "SDJWT-4.3", "OID4VP-1FINAL-B.4.5");
					}
				}

				callAndContinueOnFailure(CheckNonceInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-4.3", "OID4VP-1FINAL-B.4.5");
				callAndContinueOnFailure(ValidateSdJwtKbSdHash.class, ConditionResult.FAILURE, "SDJWT-4.3");
				callAndContinueOnFailure(CheckForUnexpectedClaimsInBindingJwt.class, ConditionResult.WARNING, "SDJWT-4.3");

				// FIXME: verify disclosures have different nonces if there are multiple

				// FIXME: verify sig on sd jwt (lissi use did:jwk though)

				// FIXME: verify credential contents?
				break;
		}
	}

	protected void populateDirectPostResponse() {
		// no redirect_uri in response, so the test ends after this response is received by wallet
		fireTestFinished();
	}

	protected void populateDirectPostResponseWithRedirectUri() {
		callAndStopOnFailure(CreateRandomCodeVerifier.class);
		callAndStopOnFailure(AddRedirectUriToDirectPostResponse.class);

		eventLog.log(getName(), "The response_uri is returning 'redirect_uri', so the wallet should send the user to that redirect_uri next");
		setStatus(Status.WAITING);
	}


	public static class CreateAuthorizationRedirectStepsUnsignedRequestUri extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
			callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class);
			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class);
		}
	}

	public static class CreateAuthorizationRedirectStepsSignedRequestUri extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
			callAndStopOnFailure(AddSelfIssuedMeV2AudToRequestObject.class);
			callAndStopOnFailure(SignRequestObjectIncludeX5cHeaderIfAvailable.class);
			callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class);
		}
	}

	protected boolean isBrowserApi() {
		switch (responseMode) {
			case DIRECT_POST:
			case DIRECT_POST_JWT:
				return false;
			case DC_API:
			case DC_API_JWT:
				break;
		}
		return true;
	}

	protected void createAuthorizationRedirect() {
		ConditionSequence seq = null;
		switch (requestMethod) {
//			case URL_QUERY:
//				callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class); // FIXME: doesn't work, Caught exception from test framework: [openid4vp://] is not a valid HTTP URL
//				break;
			case REQUEST_URI_UNSIGNED:
				if (isBrowserApi()) {
					// an alg none request object is only required for actual JAR (request_uri), for Browser API for
					// an unsigned request you just pass JSON
					return;
				}
				seq = new CreateAuthorizationRedirectStepsUnsignedRequestUri();
				break;
			case REQUEST_URI_SIGNED:
				seq = createAuthorizationRedirectStepsSignedRequestUri();
				switch (clientIdPrefix) {
					case DECENTRALIZED_IDENTIFIER:
						//Remove x5c header, only the kid header is mandatory for DIDs, which is set in the jwks parameter
						seq.replace(SignRequestObjectIncludeX5cHeaderIfAvailable.class, condition(SignRequestObjectIncludeTypHeader.class));
						break;
					case X509_SAN_DNS:
					case X509_HASH:
						// x5c header is mandatory for x509 san dns (and/or mdl profile)
						seq.replace(SignRequestObjectIncludeX5cHeaderIfAvailable.class, condition(SignRequestObjectIncludeX5cHeader.class));
						break;
					case REDIRECT_URI:
					case PRE_REGISTERED:
						// otherwise follow the default (use x5c header if it's available) although signed request objects + redirect_uri client_id_scheme isn't allowed in the spec
						break;
					case WEB_ORIGIN:
						throw new RuntimeException("web-origin client id scheme not valid for signed requests");
				}
				break;
		}
		if (isBrowserApi()) {
			seq = seq.skip(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "No redirected required for Browser API");
		}

		call(seq);
	}

	@NotNull
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		return new CreateAuthorizationRedirectStepsSignedRequestUri();
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify redirect_uri called matches the one from the response_uri response");

		callAndContinueOnFailure(CheckCallbackHttpMethodIsGet.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckUrlQueryIsEmpty.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckUrlFragmentContainsCodeVerifier.class, ConditionResult.FAILURE, "OID4VP-1FINAL-7.2");

		fireTestFinished();

		eventLog.endBlock();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING);

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);
		// FIXME add logs about the next step

		if (path.equals(env.getString("browser_api_submit", "path"))) {
			return handleBrowserApiSubmission(requestId);
		}
		if (path.equals("responseuri")) {
			return handleDirectPost(requestId);
		}
		if (path.equals(env.getString("request_uri", "path"))) {
			return handleRequestUriRequest();
		}
		return super.handleHttp(path, req, res, session, requestParts);

	}

	private Object handleBrowserApiSubmission(String requestId) {

		getTestExecutionManager().runInBackground(() -> {
			// process the callback
			setStatus(Status.RUNNING);
			call(exec().startBlock("Process Browser API result").mapKey("incoming_request", requestId));
			setStateToResponseReceived();

			switch (responseMode) {
				case DIRECT_POST:
				case DIRECT_POST_JWT:
					throw new TestFailureException(getId(), "Browser API response received but result was expected to be returned to the direct post endpoint");
				case DC_API:
				case DC_API_JWT:
					break;
			}

			callAndStopOnFailure(ExtractVP1FinalBrowserApiResponse.class);

			processReceivedResponse();

			fireTestFinished();

			return "done";
		});

		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}

	private void setStateToResponseReceived() {
		if (testState == TestState.RESPONSE_RECEIVED) {
			throw new TestFailureException(getId(), "More than one response received");
		}
		testState = TestState.RESPONSE_RECEIVED;
	}

	protected Object handleRequestUriRequest() {
		setStatus(Status.RUNNING);

		String requestObject = env.getString("request_object");

		switch (testState) {
			case INITIAL:
				markAuthorizationEndpointVisited();
				continueAfterRequestUriCalled();
				testState = TestState.REQUEST_SENT;
				break;
			case REQUEST_SENT:
				// nothing seems to prevent request_uri being retrieved more than once
				eventLog.log(getName(), "Wallet has retrieved request_uri another time");
				break;
			case RESPONSE_RECEIVED:
				throw new TestFailureException(getId(), "Wallet called request_uri after already sending a response");
		}

		setStatus(Status.WAITING);

		return ResponseEntity.ok()
			.contentType(DATAUTILS_MEDIATYPE_APPLICATION_OAUTH_OAUTHZ_REQ_JWT)
			.body(requestObject);
	}

	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - waiting for it to call the response_uri");
	}

	protected void markAuthorizationEndpointVisited() {
		// we have to manually mark this as visited as we have no way to know if/when the user scanned the qr code
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		browser.urlVisited(redirectTo);
	}

	protected String currentClientString() {
		return "";
	}

	protected boolean isSecondClient() {
		return false;
	}

}
