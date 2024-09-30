package net.openid.conformance.vp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddClientIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddIsoMdocClientMetadataToAuthorizationRequest;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPresentationDefinitionToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddRequestUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddResponseUriAsRedirectUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddResponseUriToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddSdJwtClientMetadataToAuthorizationRequest;
import net.openid.conformance.condition.client.AddSelfIssuedMeV2AudToRequestObject;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckAudInBindingJwt;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsGet;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInVpAuthorizationResponse;
import net.openid.conformance.condition.client.CheckIatInBindingJwt;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfClientIdInX509CertSanDns;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckNonceInBindingJwt;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CheckTypInBindingJwt;
import net.openid.conformance.condition.client.CheckUrlFragmentContainsCodeVerifier;
import net.openid.conformance.condition.client.CheckUrlQueryIsEmpty;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateDirectPostResponseUri;
import net.openid.conformance.condition.client.CreateEmptyAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomCodeVerifier;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.DecryptResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureIncomingRequestContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.EnsureIncomingUrlQueryIsEmpty;
import net.openid.conformance.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationEndpointResponseFromFormBody;
import net.openid.conformance.condition.client.ExtractBrowserApiResponse;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractVpToken;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticServerConfiguration;
import net.openid.conformance.condition.client.ParseVpTokenAsMdoc;
import net.openid.conformance.condition.client.ParseVpTokenAsSdJwt;
import net.openid.conformance.condition.client.RejectAuthCodeInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.SerializeRequestObjectWithNullAlgorithm;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestClientIdSchemeToRedirectUri;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestClientIdSchemeToX509SanDns;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseMode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToVpToken;
import net.openid.conformance.condition.client.SetClientIdToResponseUri;
import net.openid.conformance.condition.client.SetClientIdToResponseUriHostnameIfUnset;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeader;
import net.openid.conformance.condition.client.SignRequestObjectIncludeX5cHeaderIfAvailable;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateCredentialJWTIat;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIssIfPresentInAuthorizationResponse;
import net.openid.conformance.condition.client.ValidateSdJwtHolderBindingSignature;
import net.openid.conformance.condition.client.ValidateVpTokenIsUnpaddedBase64Url;
import net.openid.conformance.condition.client.VerifyIdTokenSubConsistentHybridFlow;
import net.openid.conformance.condition.client.WarningAboutTestingOldSpec;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CreateRandomBrowserApiSubmitUrl;
import net.openid.conformance.condition.common.CreateRandomRequestUri;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.CredentialFormat;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VPClientIdScheme;
import net.openid.conformance.variant.VPRequestMethod;
import net.openid.conformance.variant.VPResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@VariantParameters({
	CredentialFormat.class,
	VPClientIdScheme.class,
	ServerMetadata.class,
	ResponseType.class,
	VPResponseMode.class,
	VPRequestMethod.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.authorization_endpoint"
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "discovery", configurationFields = {
	"server.discoveryUrl"
})
@VariantConfigurationFields(parameter = VPClientIdScheme.class, value = "pre_registered", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = VPClientIdScheme.class, value = "x509_san_dns", configurationFields = {
	"client.client_id"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name"
})
@VariantHidesConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_secret",
	"client.jwks",
	"client2.client_secret",
	"client2.jwks"
})
public abstract class AbstractVPServerTest extends AbstractRedirectServerTestModule {

	protected ResponseType responseType;
	protected VPResponseMode responseMode;
	protected VPRequestMethod requestMethod;
	protected CredentialFormat credentialFormat;
	protected VPClientIdScheme clientIdScheme;
	protected Boolean pre_id2 = null;
	private boolean serverSupportsDiscovery;

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
		serverSupportsDiscovery = getVariant(ServerMetadata.class) == ServerMetadata.DISCOVERY;

		responseType = getVariant(ResponseType.class);
		env.putString("response_type", responseType.toString());

		responseMode = getVariant(VPResponseMode.class);
		env.putString("response_mode", responseMode.toString());
		credentialFormat = getVariant(CredentialFormat.class);
		requestMethod = getVariant(VPRequestMethod.class);
		clientIdScheme = getVariant(VPClientIdScheme.class);

		// As per ISO 18013-7 B.5.3 "Nonces shall have a minimum length of 16 bytes"
		env.putInteger("requested_nonce_length", 16);

		switch (responseMode) {
			case DIRECT_POST:
			case DIRECT_POST_JWT:
				callAndStopOnFailure(CreateDirectPostResponseUri.class);
				break;
			case W3C_DC_API_JWT:
			case W3C_DC_API:
				break;
		}

		switch (clientIdScheme) {
			case PRE_REGISTERED:
				// client id has been set already in config
				break;
			case REDIRECT_URI:
				callAndStopOnFailure(SetClientIdToResponseUri.class);
				break;
			case X509_SAN_DNS:
				callAndStopOnFailure(SetClientIdToResponseUriHostnameIfUnset.class);
				break;
		}

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("response_uri");

		pre_id2 = env.getBoolean("config", "pre_id2");
		if (pre_id2 == null) {
			pre_id2 = false;
		}
		if (pre_id2) {
			callAndContinueOnFailure(WarningAboutTestingOldSpec.class, ConditionResult.WARNING);
		}

		switch (getVariant(ServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(GetDynamicServerConfiguration.class);
				break;
			case STATIC:
				callAndStopOnFailure(GetStaticServerConfiguration.class);
				break;
		}

		// make sure the server configuration passes some basic sanity checks
		//callAndStopOnFailure(CheckServerConfiguration.class); // FIXME doesn't like the openid4vp:// url being set as authorization endpoint url

//		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class); // FIXME doesn't like the openid4vp:// url being set as authorization endpoint url

//		callAndStopOnFailure(FetchServerKeys.class); is there a jwks uri?
//		callAndContinueOnFailure(CheckServerKeysIsValid.class, Condition.ConditionResult.WARNING);
		// Includes verify-base64url and bare-keys assertions (OIDC test)
		//callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		//callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		//callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
		//callAndContinueOnFailure(EnsureServerJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7518-6.3.2.1");

//		skipTestIfSigningAlgorithmNotSupported();

		// Set up the client configuration
		configureClient();

		skipTestIfScopesNotSupported();

		// Set up the resource endpoint configuration
//		callAndStopOnFailure(SetProtectedResourceUrlToUserInfoEndpoint.class);
		if (credentialFormat == CredentialFormat.ISO_MDL) {
			// ISO spec always creates a redirect returned from response_uri
			callAndStopOnFailure(CreateRedirectUri.class);
		}

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void skipTestIfSigningAlgorithmNotSupported() {
		// Just apply for 'oidcc-idtoken-unsigned' test
	}

	protected void skipTestIfScopesNotSupported() {
		// Just apply for scope tests
	}

	protected void skipTestIfNoneUnsupported() {
		JsonElement el = env.getElementFromObject("server", "request_object_signing_alg_values_supported");
		if (el != null && el.isJsonArray()) {
			JsonArray serverValues = el.getAsJsonArray();
			if (!serverValues.contains(new JsonPrimitive("none"))) {
				fireTestSkipped("'none' is not listed in request_object_signing_alg_values_supported - assuming it is not supported.");
			}
		}
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3");
		callAndStopOnFailure(CreateRandomRequestUri.class, "OIDCC-6.2");
		browser.setShowQrCodes(true);
	}

	protected void configureClient() {
		// Set up the client configuration
		switch (getVariant(ClientRegistration.class)) {
		case STATIC_CLIENT:
			callAndStopOnFailure(GetStaticClientConfiguration.class);
			configureStaticClient();
			break;
		case DYNAMIC_CLIENT:
			callAndStopOnFailure(StoreOriginalClientConfiguration.class);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			configureDynamicClient();
			break;
		}

		exposeEnvString("client_id");

		completeClientConfiguration();
	}

	protected void configureStaticClient() {
		JsonElement el = env.getElementFromObject("client", "jwks");
		if (el == null) {
			boolean jwksRequired = false;
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
				case W3C_DC_API:
					break;
				case DIRECT_POST_JWT:
				case W3C_DC_API_JWT:
					// assume response is encrypted so a key is required
					jwksRequired = true;
					break;
			}

			if (!jwksRequired) {
				return;
			}
		}
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, ConditionResult.FAILURE, "RFC7517-4.5");
	}

	protected void createDynamicClientRegistrationRequest() {

		// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_Dynamic
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType));

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

		callAndStopOnFailure(CreateRandomRequestUri.class, "OIDCC-6.2");
		callAndStopOnFailure(AddRequestUriToDynamicRegistrationRequest.class);
	}

	protected void configureDynamicClient() {

		createDynamicClientRegistrationRequest();

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
	}

	protected void completeClientConfiguration() {
		if (clientIdScheme == VPClientIdScheme.X509_SAN_DNS) {
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
			case W3C_DC_API:
			case W3C_DC_API_JWT:
				callAndStopOnFailure(CreateRandomBrowserApiSubmitUrl.class);
				String submitUrl = env.getString("browser_api_submit", "fullUrl");

				JsonObject request;
				request = env.getObject("authorization_endpoint_request");

				eventLog.log(getName(), args("msg", "Calling browser API",
					"request", request,
					"http", "api"));

				browser.requestCredential(request, submitUrl);
				setStatus(Status.WAITING);

				eventLog.log(getName(), "The wallet should be opened using the Browser API button, and should then fetch the request_uri");
				break;
			case DIRECT_POST_JWT:
			case DIRECT_POST:
				performRedirect();
				eventLog.log(getName(), "The wallet should be opened via the QR code / proceed with test button, and should then fetch the request_uri");
				break;
		}
		eventLog.endBlock();
	}

	public static class CreateAuthorizationRequestSteps extends AbstractConditionSequence {
		private VPResponseMode responseMode;
		private CredentialFormat credentialFormat;
		private VPClientIdScheme clientIdScheme;

		public CreateAuthorizationRequestSteps(VPResponseMode responseMode, CredentialFormat credentialFormat, VPClientIdScheme clientIdScheme) {
			this.responseMode = responseMode;
			this.credentialFormat = credentialFormat;
			this.clientIdScheme = clientIdScheme;
		}

		@Override
		public void evaluate() {
			//boolean browserApi = false;
			boolean browserUnsigned = false;
			switch (responseMode) {
				case DIRECT_POST:
				case DIRECT_POST_JWT:
					break;
				case W3C_DC_API:
					browserUnsigned = true;
					//browserApi = true;
					break;
				case W3C_DC_API_JWT:
					//browserApi = true;
					break;
			}

			callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
			if (!browserUnsigned) {
				callAndStopOnFailure(AddClientIdToAuthorizationEndpointRequest.class);

				callAndStopOnFailure(CreateRandomStateValue.class);
				call(exec().exposeEnvironmentString("state"));
				callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

				callAndStopOnFailure(AddResponseUriToAuthorizationEndpointRequest.class);
			}

			callAndStopOnFailure(AddPresentationDefinitionToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(CreateRandomNonceValue.class);
			call(exec().exposeEnvironmentString("nonce"));
			callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

			switch (credentialFormat) {
				case ISO_MDL:
					callAndStopOnFailure(AddIsoMdocClientMetadataToAuthorizationRequest.class);
					break;
				case SD_JWT_VC:
					callAndStopOnFailure(AddSdJwtClientMetadataToAuthorizationRequest.class);
					break;
			}

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToVpToken.class);

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseMode.class);

			switch (clientIdScheme) {
				case PRE_REGISTERED:
					// pre-registered is the default, we can omit the client_id_scheme
					// FIXME: try passing client_id_scheme=pre-registered for one of tests
					break;
				case REDIRECT_URI:
					callAndStopOnFailure(SetAuthorizationEndpointRequestClientIdSchemeToRedirectUri.class, "OID4VP-5.7");
					break;
				case X509_SAN_DNS:
					// use x509_san_dns as per the only one that's supported B.3.1.3.1	Static set of Wallet Metadata in IOS 18013-7
					callAndStopOnFailure(SetAuthorizationEndpointRequestClientIdSchemeToX509SanDns.class, "OID4VP-5.7");
					break;
			}

		}
	}

	protected void createAuthorizationRequest() {
		call(createAuthorizationRequestSequence());
	}

	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = new CreateAuthorizationRequestSteps(responseMode, credentialFormat, clientIdScheme);

		if (pre_id2) {
			createAuthorizationRequestSteps = createAuthorizationRequestSteps.
				replace(AddResponseUriToAuthorizationEndpointRequest.class,
					condition(AddResponseUriAsRedirectUriToAuthorizationEndpointRequest.class));
		}

		return createAuthorizationRequestSteps;
	}

	protected Object handleDirectPost(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Direct post endpoint").mapKey("incoming_request", requestId));
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureIncomingRequestContentTypeIsFormUrlEncoded.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureIncomingUrlQueryIsEmpty.class, ConditionResult.FAILURE);

		switch (responseMode) {
			case DIRECT_POST:
				callAndStopOnFailure(ExtractAuthorizationEndpointResponseFromFormBody.class, ConditionResult.FAILURE);
				break;
			case DIRECT_POST_JWT:
				// currently only supports encrypted-not-signed as used by mdl
				// FIXME: verify no parameters other than response
				callAndStopOnFailure(DecryptResponse.class);
				// FIXME: need to validate jwe header
				// FIXME iss, exp and aud MUST be omitted in the JWT Claims Set of the JWE
				break;
			case W3C_DC_API:
			case W3C_DC_API_JWT:
				throw new TestFailureException(getId(), "Direct post response received but result was expected to be returned from the Browser API");
		}

		processReceivedResponse();

		// as per https://openid.bitbucket.io/connect/openid-4-verifiable-presentations-1_0.html#section-6.2
		JsonObject response = new JsonObject();
		switch (credentialFormat) {
			case ISO_MDL:
				// iso mdl spec requires that redirect uri is always returned
				populateDirectPostResponseWithRedirectUri(response);
				break;
			default:
				populateDirectPostResponse(response);
				break;
		}

		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.body(response.toString());
	}

	private void processReceivedResponse() {
		// FIXME: decryption doesn't work for browser API

		// vp token may be an object containing multiple tokens, https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#section-6.1
		// however I think we would only get multiple tokens if they were explicitly requested, so we can safely assume only a single token here
		callAndStopOnFailure(ExtractVpToken.class, ConditionResult.FAILURE);

		// FIXME: extract / verify presentation_submission

		callAndContinueOnFailure(CheckForUnexpectedParametersInVpAuthorizationResponse.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE, "OIDCC-3.2.2.5");

		switch (credentialFormat) {
			case ISO_MDL:
				// mdoc
				callAndContinueOnFailure(ValidateVpTokenIsUnpaddedBase64Url.class, ConditionResult.FAILURE);
				callAndStopOnFailure(ParseVpTokenAsMdoc.class);
				break;

			case SD_JWT_VC:
				callAndStopOnFailure(ParseVpTokenAsSdJwt.class, ConditionResult.FAILURE);

				eventLog.startBlock(currentClientString() + "Verify credential JWT");
				// as per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2 these must must not be selectively disclosed
				// FIXME check iss is a valid uri
				callAndContinueOnFailure(ValidateCredentialJWTIat.class, ConditionResult.FAILURE, "SDJWTVC-4.2.2.2");
				// FIXME nbf
				// FIXME exp
				// cnf is checked when holder binding is checked below
				// FIXME type
				// FIXME status

				eventLog.startBlock(currentClientString() + "Verify holder binding JWT");
				// https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-05.html#name-key-binding-jwt

				callAndContinueOnFailure(ValidateSdJwtHolderBindingSignature.class, ConditionResult.FAILURE, "SDJWT-5.10");

				callAndContinueOnFailure(CheckTypInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-5.10");
				// alg is checked during signature validation

				callAndContinueOnFailure(CheckIatInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-5.10");
				callAndContinueOnFailure(CheckAudInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-5.10");
				callAndContinueOnFailure(CheckNonceInBindingJwt.class, ConditionResult.FAILURE, "SDJWT-5.10");

				// FIXME: verify disclosures have different nonces if there are multiple

				// FIXME: verify sig on sd jwt (lissi use did:jwk though)

				// FIXME: verify credential contents?
				break;
		}
	}

	protected void populateDirectPostResponse(JsonObject response) {
		// no redirect_uri in response, so the test ends after this response is received by wallet
		fireTestFinished();
	}

	protected void populateDirectPostResponseWithRedirectUri(JsonObject response) {
		callAndStopOnFailure(CreateRandomCodeVerifier.class);
		response.addProperty("redirect_uri", env.getString("redirect_uri") + "#" + env.getString("code_verifier"));

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

	protected void createAuthorizationRedirect() {
		ConditionSequence seq = null;
		switch (requestMethod) {
//			case URL_QUERY:
//				callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class); // FIXME: doesn't work, Caught exception from test framework: [openid4vp://] is not a valid HTTP URL
//				break;
			case REQUEST_URI_UNSIGNED:
				seq = new CreateAuthorizationRedirectStepsUnsignedRequestUri();
				break;
			case REQUEST_URI_SIGNED:
				seq = createAuthorizationRedirectStepsSignedRequestUri();
				switch (clientIdScheme) {
					case X509_SAN_DNS:
						// x5c header is mandatory for x509 san dns (and/or mdl profile)
						seq.replace(SignRequestObjectIncludeX5cHeaderIfAvailable.class, condition(SignRequestObjectIncludeX5cHeader.class));
						break;
					case REDIRECT_URI:
					case PRE_REGISTERED:
						// otherwise follow the default (use x5c header if it's available) although signed request objects + redirect_uri client_id_scheme isn't allowed in the spec
						break;
				}
				break;
		}
		switch (responseMode) {
			case DIRECT_POST:
			case DIRECT_POST_JWT:
				break;
			case W3C_DC_API:
			case W3C_DC_API_JWT:
				seq = seq.skip(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "No redirected required for Browser API");
				break;
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
		callAndContinueOnFailure(CheckUrlFragmentContainsCodeVerifier.class, ConditionResult.FAILURE, "OID4VP-");

		fireTestFinished();

//		if (formPost) {
//			env.mapKey("authorization_endpoint_response", "callback_body_form_params");
//			callAndContinueOnFailure(CheckCallbackHttpMethodIsPost.class, ConditionResult.FAILURE, "OAuth2-FP-2");
//			callAndContinueOnFailure(CheckCallbackContentTypeIsFormUrlEncoded.class, ConditionResult.FAILURE, "OAuth2-FP-2");
//			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");
//			callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");
//		} else if (isCodeFlow()) {
//			env.mapKey("authorization_endpoint_response", "callback_query_params");
//		} else {
//			env.mapKey("authorization_endpoint_response", "callback_params");
//
//			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");
//			callAndContinueOnFailure(RejectErrorInUrlQuery.class, ConditionResult.FAILURE, "OAuth2-RT-5");
//		}
//
//		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE);
		if (responseType.includesCode()) {
			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		}
		if (responseType.includesToken()) {
			callAndStopOnFailure(ExtractAccessTokenFromAuthorizationResponse.class);
		}
		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		if (responseType.includesIdToken()) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class);

			// save the id_token returned from the authorization endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

			performAuthorizationEndpointIdTokenValidation();
		}
		if (responseType.includesCode()) {
			performAuthorizationCodeValidation();
		}
		if (responseType.includesToken()) {
			requestProtectedResource();
		}
		performPostAuthorizationFlow();
	}

	protected void performAuthorizationEndpointIdTokenValidation() {
		performIdTokenValidation();
	}

	protected void performIdTokenValidation() {
		call(new PerformStandardIdTokenChecks());
	}

	protected void performAuthorizationCodeValidation() {
	}

	protected void performPostAuthorizationFlow() {
		onPostAuthorizationFlowComplete();
	}

	protected void requestAuthorizationCode() {
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.INFO, "RFC6749-5.1"); // this is 'recommended' by the RFC, but we don't want to raise a warning on every test
		skipIfMissing(new String[] { "expires_in" }, null, ConditionResult.INFO,
			ValidateExpiresIn.class, ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class, ConditionResult.INFO);

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.1.3.3", "OIDCC-3.3.3.3");

		// save the id_token returned from the token endpoint
		env.putObject("token_endpoint_id_token", env.getObject("id_token"));

		additionalTokenEndpointResponseValidation();

		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(VerifyIdTokenSubConsistentHybridFlow.class, ConditionResult.FAILURE, "OIDCC-2");
		}
	}

	protected void additionalTokenEndpointResponseValidation() {
		performIdTokenValidation();
	}

	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResource.class);
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		eventLog.endBlock();
	}

	/**
	 * Do generic checks on an error response from the authorization endpoint
	 *
	 * Generally called from onAuthorizationCallbackResponse. The caller stills needs to check for the exact specific
	 * error code their test scenario expects.
	 */
	protected void performGenericAuthorizationEndpointErrorResponseValidation() {
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(RejectAuthCodeInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB.class, ConditionResult.WARNING, "RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorDescriptionFromAuthorizationEndpointResponseError.class, ConditionResult.FAILURE,"RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorUriFromAuthorizationEndpointResponseError.class, ConditionResult.FAILURE,"RFC6749-4.1.2.1");
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}


	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

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

			callAndStopOnFailure(ExtractBrowserApiResponse.class);

			processReceivedResponse();

			fireTestFinished();

			return "done";
		});

		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}

	protected Object handleRequestUriRequest() {
		setStatus(Status.RUNNING);
		markAuthorizationEndpointVisited();

		String requestObject = env.getString("request_object");

		eventLog.log(getName(), "Wallet has retrieved request_uri - waiting for it to call the response_uri");

		setStatus(Status.WAITING);

		return ResponseEntity.ok()
			.contentType(DATAUTILS_MEDIATYPE_APPLICATION_OAUTH_OAUTHZ_REQ_JWT)
			.body(requestObject);
	}

	protected void markAuthorizationEndpointVisited() {
		// we have to manually mark this as visited as we have no way to know if/when the user scanned the qr code
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		browser.urlVisited(redirectTo);
	}


	@Override
	public void cleanup() {
		unregisterClient();
	}

	public void unregisterClient() {
		eventLog.startBlock(currentClientString() + "Unregister dynamically registered client");

		call(condition(UnregisterDynamicallyRegisteredClient.class)
			.skipIfObjectsMissing(new String[] {"client"})
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure());

		eventLog.endBlock();
	}

	protected String currentClientString() {
		return "";
	}

	protected boolean isSecondClient() {
		return false;
	}

	protected boolean serverSupportsDiscovery() {
		return serverSupportsDiscovery;
	}

	protected boolean isCodeFlow() {
		return responseType.equals(ResponseType.CODE);
	}

	protected boolean isHybridFlow() {
		return responseType.includesCode() && !isCodeFlow();
	}

	protected boolean isImplicitFlow() {
		return !responseType.includesCode();
	}
}
