package net.openid.conformance.authzen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.openid.conformance.authzen.condition.AddApiKeyAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddXRequestIdHeaderToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAllowingJsonParseFailure;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAllowingTlsRejection;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifyExpectedStatus;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiEndpointPathContainsV1;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiResponseXRequestIdMatches;
import net.openid.conformance.authzen.condition.EnsureHttpStatusCodeMatchesExpected;
import net.openid.conformance.authzen.condition.EnsureMethodNotAllowedResponseAllowsPost;
import net.openid.conformance.authzen.condition.WarnMethodRejectedWithNotFound;
import net.openid.conformance.authzen.condition.WarnUnsupportedMethodAccepted;
import net.openid.conformance.authzen.condition.EnsurePDPMetadataAdvertisesEndpoint;
import net.openid.conformance.authzen.condition.CorruptAuthzenClientCredentials;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestFromRaw;
import net.openid.conformance.authzen.condition.GenerateUntrustedAuthzenMtlsClientCertificate;
import net.openid.conformance.authzen.condition.GetAuthzenClientConfiguration;
import net.openid.conformance.authzen.condition.GetPDPStaticServerConfiguration;
import net.openid.conformance.authzen.condition.ValidatePDPIdentifier;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.RemoveMTLSCertificates;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.PDPAuthType;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	PDPServerMetadata.class,
	PDPAuthType.class,
	AuthzenSupport.class
})
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.policy_decision_point"
})
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "discovery", configurationFields = {
	"pdp.policy_decision_point",
	"pdp.jwks",
	"pdp.metadata_issuer"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_id",
	"client.client_secret"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "api_key", configurationFields = {
	"client.api_key"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca"
})


public abstract class AbstractAuthzenPDPTest extends AbstractAuthzenPDPMetadataTest {
	protected boolean serverSupportsDiscovery;
	protected Class<? extends ConditionSequence> profileCompleteClientConfiguration;
	protected Class<? extends ConditionSequence> addPDPEndpointClientAuthentication;

	private boolean pdpAuthUsesMtls;
	protected Class<? extends Condition> loadClientConfiguration;
	private final Set<String> endpointPathsV1Checked = new HashSet<>();

	public static class ConfigureClientForMtls extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, ConditionResult.WARNING);
			callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, ConditionResult.FAILURE);
		}
	}

	public static class AddBasicAuthClientSecretAuthenticationToAuthzenApiRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest.class);
		}
	}

	public static class AddApiKeyAuthenticationToAuthzenApiRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddApiKeyAuthenticationParametersToAuthzenApiRequest.class);
		}
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "none")
	public void setupNone() {
		profileCompleteClientConfiguration = null;
		addPDPEndpointClientAuthentication = null;
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		profileCompleteClientConfiguration = null;
		addPDPEndpointClientAuthentication = AddBasicAuthClientSecretAuthenticationToAuthzenApiRequest.class;
		loadClientConfiguration = GetAuthzenClientConfiguration.class;
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "api_key")
	public void setupApiKey() {
		profileCompleteClientConfiguration = null;
		addPDPEndpointClientAuthentication = AddApiKeyAuthenticationToAuthzenApiRequest.class;
		loadClientConfiguration = GetAuthzenClientConfiguration.class;
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "mtls")
	public void setupMtls() {
		profileCompleteClientConfiguration = ConfigureClientForMtls.class;
		// The client certificate is presented in the TLS handshake of every API call, from the
		// mutual_tls_authentication object ConfigureClientForMtls stores; the request itself
		// carries no credentials.
		addPDPEndpointClientAuthentication = null;
		pdpAuthUsesMtls = true;
	}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		if (!storeConfigurationUnlessSkipped(config, baseUrl, externalUrlOverride, baseMtlsUrl)) {
			return;
		}
		serverSupportsDiscovery = getVariant(PDPServerMetadata.class) == PDPServerMetadata.DISCOVERY;

		PDPAuthType PdpAuthType = getVariant(PDPAuthType.class);
		env.putString("client_auth_type", PdpAuthType.toString());

		callAndStopOnFailure(ValidatePDPIdentifier.class, "AUTHZEN-9.1.1");

		switch (getVariant(PDPServerMetadata.class)) {
			case DISCOVERY:
				fetchAndValidateDiscoveryMetadata();
				break;
			case STATIC:
				callAndStopOnFailure(GetPDPStaticServerConfiguration.class);
				break;
		}

		checkPDPServerConfiguration(serverSupportsDiscovery);
		if (serverSupportsDiscovery) {
			eventLog.startBlock("Check the PDP metadata advertises the API endpoints the test calls");
			try {
				callAndStopOnFailure(new EnsurePDPMetadataAdvertisesEndpoint(getEndpointMetadataParameter()), "AUTHZEN-9.1.1", "AUTHZEN-10.1");
				for (String endpointParameter : getOtherEndpointMetadataParameters()) {
					callAndStopOnFailure(new EnsurePDPMetadataAdvertisesEndpoint(endpointParameter), "AUTHZEN-9.1.1", "AUTHZEN-10.1");
				}
			} finally {
				eventLog.endBlock();
			}
		}

		// Set up the client configuration
		configureClient();

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		// No custom configuration
	}

	protected void configureClient() {
		completeClientConfiguration();
	}

	protected void completeClientConfiguration() {
		if (loadClientConfiguration != null) {
			callAndStopOnFailure(loadClientConfiguration);
		}
		if(profileCompleteClientConfiguration != null) {
			call(sequence(profileCompleteClientConfiguration));
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performAuthzenApiFlow();
	}

	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Make request to API endpoint");
		createAuthzenApiRequest();
		performSingleApiRequest();
		// The response body is validated whenever the PDP answered 200 and 200 is acceptable. A
		// module that also accepts an error status validates no body when it gets one.
		if (getAcceptableHttpStatusCodes().contains(200) && responseStatusIs200()) {
			processAuthApiEndpointResponse();
			validateAuthApiEndpointResponse();
		}
		performPostApiFlow();
		eventLog.endBlock();
	}

	private boolean responseStatusIsSuccess() {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		return status != null && status >= 200 && status < 300;
	}

	protected boolean responseStatusIs200() {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		return status != null && status == 200;
	}

	/**
	 * Apply request overrides, add the X-Request-ID header when enabled, dispatch
	 * the API call, and assert the X-Request-ID echo. The per-iteration loops in
	 * {@link #runIdempotencyLoop} and
	 * {@link AbstractAuthzenPDPSearchTest#runPaginatedSearchLoop} call this
	 * method each iteration so per-request behaviors apply every time.
	 *
	 * <p>Does NOT call {@link #createAuthzenApiRequest()}; callers decide whether
	 * to rebuild the request body each iteration or reuse the previous one.
	 */
	protected void performSingleApiRequest() {
		applyRequestOverrides();
		if (includeXRequestIdHeader()) {
			callAndStopOnFailure(AddXRequestIdHeaderToAuthzenApiRequest.class, "AUTHZEN-10.1.3");
		}
		callAuthApiEndpointRequest();
		if (!"POST".equals(getRequestHttpMethod())) {
			// Working-group guidance for a request with a method other than POST: 405 (with Allow:
			// POST) is preferred, or 400; 404 only when the framework routes on method and path.
			callAndContinueOnFailure(WarnUnsupportedMethodAccepted.class, ConditionResult.WARNING, "AUTHZEN-10.1");
			callAndContinueOnFailure(WarnMethodRejectedWithNotFound.class, ConditionResult.WARNING, "AUTHZEN-10.1");
			callAndContinueOnFailure(EnsureMethodNotAllowedResponseAllowsPost.class, ConditionResult.WARNING, "AUTHZEN-10.1", "RFC7231-6.5.5");
		}
		if (includeXRequestIdHeader() && !pdpRejectedAtTlsLayer()) {
			callAndContinueOnFailure(EnsureAuthzenApiResponseXRequestIdMatches.class, ConditionResult.FAILURE, "AUTHZEN-10.1.3");
		}
	}

	/**
	 * Standard 3-iteration idempotency loop used by every API family
	 * (Evaluation / Evaluations / Subject Search / Resource Search / Action Search).
	 * Each idempotency abstract base delegates to this method, passing its own
	 * API-family label and the capture/match condition classes appropriate for
	 * that family (decision, batch decisions or search results).
	 *
	 * <p>Log blocks do not nest, so each iteration logs its request in one block, or in the page
	 * blocks a search opens, and records or compares the answer in a block of its own. Every
	 * block is closed in a finally, so it closes even when a condition throws.
	 */
	protected void runIdempotencyLoop(String apiFamilyLabel,
			Class<? extends Condition> capture,
			Class<? extends Condition> match) {
		final int iterations = 3;
		for (int i = 1; i <= iterations; i++) {
			eventLog.startBlock("Idempotency iteration " + i + " of " + iterations + ": send the " + apiFamilyLabel + " request");
			try {
				resetStateBetweenIdenticalRequests();
				performIdempotencyIteration();
			} finally {
				eventLog.endBlock();
			}
			eventLog.startBlock(i == 1
				? "Idempotency iteration 1: record the first answer"
				: "Idempotency iteration " + i + ": compare with the first answer");
			try {
				if (i == 1) {
					callAndStopOnFailure(capture);
				} else {
					// Idempotency is required by certification scenario 5.6 ("The PDP MUST
					// return the same decision value each time"), not by the Authorization
					// API specification, which does not address it. The scenario has no
					// requirement link prefix, so this assertion carries no tag.
					callAndContinueOnFailure(match, getIdempotencySeverity());
				}
			} finally {
				eventLog.endBlock();
			}
		}
		performPostApiFlow();
	}

	/**
	 * Sends one of the identical requests of {@link #runIdempotencyLoop} and validates the
	 * response, leaving it where the loop's capture and match conditions read it.
	 */
	protected void performIdempotencyIteration() {
		createAuthzenApiRequest();
		performSingleApiRequest();
		processAuthApiEndpointResponse();
		validateAuthApiEndpointResponse();
	}

	/**
	 * Clears state an earlier response left behind that would otherwise change the next request
	 * built by {@link #runIdempotencyLoop}, so every iteration sends the same request.
	 */
	protected void resetStateBetweenIdenticalRequests() {
	}

	/**
	 * Severity of a response that differs from the first one in {@link #runIdempotencyLoop}.
	 */
	protected ConditionResult getIdempotencySeverity() {
		return ConditionResult.FAILURE;
	}

	/**
	 * Whether to add an `X-Request-ID` header to the request; when true, the response is
	 * asserted to echo the same value (Section 10.1.3). The echo is required on every response
	 * to an Authorization API request, including errors, so by default a module that expects an
	 * error status sends one. A module that expects success does not, so the plans also exercise
	 * requests without the header (certification scenario 5.5.2); the X-Request-ID modules
	 * override this for the success case. A GET or PUT is not a request in the POST binding of
	 * Section 10.1, so the method-rejection modules send none.
	 */
	protected boolean includeXRequestIdHeader() {
		return "POST".equals(getRequestHttpMethod()) && !getAcceptableHttpStatusCodes().contains(200);
	}

	/**
	 * Override to send a different HTTP method (e.g. "GET", "PUT"). Default is POST.
	 */
	protected String getRequestHttpMethod() {
		return "POST";
	}

	/**
	 * Override the `Content-Type` request header. Return null to use the default
	 * `application/json`. Return the empty string to omit the header entirely.
	 */
	protected String getRequestContentTypeOverride() {
		return null;
	}

	/**
	 * Override to send a raw body string (e.g. malformed JSON, an empty body, or a
	 * top-level JSON array). When non-null, this body is transmitted verbatim and the
	 * JSON object stored in `authzen_api_endpoint_request` is not serialized.
	 */
	protected String getRawRequestBody() {
		return null;
	}

	/**
	 * Under mtls, a module that omits or corrupts the credentials sends no client certificate,
	 * or one no CA vouches for, which the PDP may refuse in the TLS handshake.
	 */
	private boolean sendsUnacceptableClientCertificate() {
		return pdpAuthUsesMtls && (skipAuthentication() || corruptAuthCredentials());
	}

	/** True when the PDP refused the last call in the TLS handshake, so there is no HTTP response. */
	protected boolean pdpRejectedAtTlsLayer() {
		return Boolean.TRUE.equals(env.getBoolean(CallAuthzenApiEndpointAllowingTlsRejection.TLS_REJECTED_KEY));
	}

	/**
	 * Override to skip adding client authentication credentials to the request. Used
	 * by negative tests that verify the PDP returns 401 when called without auth.
	 */
	protected boolean skipAuthentication() {
		return false;
	}

	/**
	 * Override to overwrite client_secret / api_key with a deliberately invalid value
	 * before the auth condition runs. Used by 401 negative tests where the request
	 * must arrive at the PDP carrying syntactically-valid-but-wrong credentials.
	 */
	protected boolean corruptAuthCredentials() {
		return false;
	}

	/**
	 * Translate the test-level hooks above into the env keys that CallAuthzenApiEndpoint
	 * reads. Runs after the request body is built and before the API call is made.
	 */
	protected void applyRequestOverrides() {
		String method = getRequestHttpMethod();
		if (!"POST".equals(method)) {
			env.putString("authzen_api_endpoint_request_method", method);
		}
		String contentType = getRequestContentTypeOverride();
		if (contentType != null) {
			env.putString("authzen_api_endpoint_request_content_type", contentType);
		}
		String rawBody = getRawRequestBody();
		if (rawBody != null) {
			env.putString("authzen_api_endpoint_request_raw_body", rawBody);
		}
	}

	protected void callAuthApiEndpointRequest() {
		setAuthzenApiEndpoint();
		checkEndpointPathContainsV1();
		addAuthenticationToAuthzenApiEndpoint();
		try {
			performApiRequestCall();
		} finally {
			// An untrusted client certificate mapped in by addAuthenticationToAuthzenApiEndpoint is for
			// this call only.
			env.unmapKey("mutual_tls_authentication");
		}
	}

	protected void performApiRequestCall() {
		Set<Integer> acceptable = getAcceptableHttpStatusCodes();
		if (acceptable.size() == 1 && acceptable.iterator().next().intValue() == 200) {
			call(new CallAuthzenApiEndpointAndVerifySuccessfulResponse()
				.replace(EnsureHttpStatusCodeIs200.class, condition(EnsureHttpStatusCodeIs200.class)
					.requirements(expectedStatusRequirements(acceptable))
					.onFail(ConditionResult.FAILURE)
					.dontStopOnFailure()));
		} else {
			JsonObject wrapper = new JsonObject();
			JsonArray codes = new JsonArray();
			for (Integer code : acceptable) {
				codes.add(code);
			}
			wrapper.add("codes", codes);
			env.putObject("authzen_expected_http_status_codes", wrapper);
			if (!"POST".equals(getRequestHttpMethod())) {
				// Accepting the request is a warning (WarnUnsupportedMethodAccepted, run after the
				// call); any other status outside the acceptable set, such as a 5xx, fails.
				callAndStopOnFailure(CallAuthzenApiEndpointAllowingJsonParseFailure.class, "AUTHZEN-10");
				if (!responseStatusIsSuccess()) {
					env.mapKey("endpoint_response", "authzen_api_endpoint_response");
					callAndContinueOnFailure(EnsureHttpStatusCodeMatchesExpected.class, ConditionResult.FAILURE, expectedStatusRequirements(acceptable));
					env.unmapKey("endpoint_response");
				}
				return;
			}
			if (sendsUnacceptableClientCertificate()) {
				callAndStopOnFailure(CallAuthzenApiEndpointAllowingTlsRejection.class, "AUTHZEN-11.3");
				if (!pdpRejectedAtTlsLayer()) {
					env.mapKey("endpoint_response", "authzen_api_endpoint_response");
					callAndContinueOnFailure(EnsureHttpStatusCodeMatchesExpected.class, ConditionResult.FAILURE, expectedStatusRequirements(acceptable));
					env.unmapKey("endpoint_response");
				}
				return;
			}
			call(new CallAuthzenApiEndpointAndVerifyExpectedStatus(expectedStatusRequirements(acceptable)));
			if (acceptable.contains(200) && responseStatusIs200()) {
				// A module that accepts success or an error checks a success like the success-only
				// path does: "A successful response is an HTTPS response with a status code of 200
				// and a Content-Type of application/json" (Section 10.1).
				env.mapKey("endpoint_response", "authzen_api_endpoint_response");
				callAndContinueOnFailure(EnsureContentTypeJson.class, ConditionResult.FAILURE, "AUTHZEN-10.1");
				env.unmapKey("endpoint_response");
			}
		}
	}

	/**
	 * Override to assert against a non-200 expected HTTP status code (e.g. 400, 401).
	 * When the expected code is not 200, response-body parsing and validation are skipped
	 * and the test only asserts that the actual status matches. Tests that accept a class
	 * of 4xx responses (e.g. method-not-allowed could be 400 or 405) should override
	 * {@link #getAcceptableHttpStatusCodes()} instead.
	 */
	protected int getExpectedHttpStatusCode() {
		return 200;
	}

	/**
	 * Override to assert the actual HTTP status is in a set of acceptable codes. Default
	 * delegates to {@link #getExpectedHttpStatusCode()} as a singleton set, so existing
	 * exact-code tests keep working. A module that accepts several outcomes returns them all, as
	 * the method rejection modules do ({@code Set.of(400, 404, 405)}, with 404 warned about) and
	 * the media type rejection modules do ({@code Set.of(400, 415)}).
	 */
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return Set.of(getExpectedHttpStatusCode());
	}

	/**
	 * The sections requiring the status a module expects: Section 10.1, the HTTPS POST binding, for
	 * a request with another method or the wrong media type, Section 11.3 for 401, Sections 10.1.1
	 * and 10.1.2 for the 400 a malformed request gets, otherwise Section 10.1; preceded by
	 * {@link #additionalStatusRequirements()}.
	 */
	protected String[] expectedStatusRequirements(Set<Integer> acceptable) {
		String[] byStatus = statusRequirements(acceptable);
		String[] additional = additionalStatusRequirements();
		String[] requirements = Arrays.copyOf(additional, additional.length + byStatus.length);
		System.arraycopy(byStatus, 0, requirements, additional.length, byStatus.length);
		return requirements;
	}

	/**
	 * Sections of the specification that the module's request tests, beyond the one that says
	 * which status answers it; they are listed first on the status check, whichever statuses the
	 * module accepts.
	 */
	protected String[] additionalStatusRequirements() {
		return new String[0];
	}

	private String[] statusRequirements(Set<Integer> acceptable) {
		if (!"POST".equals(getRequestHttpMethod())) {
			return new String[] {"AUTHZEN-10.1"};
		}
		if (getRequestContentTypeOverride() != null && !acceptable.contains(200)) {
			// The request's media type is wrong: the Content-Type requirement is in Section 10.1.
			return new String[] {"AUTHZEN-10.1"};
		}
		if (acceptable.contains(401)) {
			return new String[] {"AUTHZEN-11.3"};
		}
		if (acceptable.contains(400)) {
			return new String[] {"AUTHZEN-10.1.1", "AUTHZEN-10.1.2"};
		}
		return new String[] {"AUTHZEN-10.1"};
	}

	/**
	 * Override to send the request payload exactly as returned by {@link #getPayload()},
	 * bypassing the Create*Steps sequences that strip unknown fields and enforce
	 * required ones client-side. Used by negative tests that need to deliver an
	 * intentionally malformed payload to the PDP.
	 */
	protected boolean sendRawRequest() {
		return false;
	}

	protected abstract void processAuthApiEndpointResponse();

	protected abstract void validateAuthApiEndpointResponse();

	protected void createAuthzenApiRequest() {
		if (sendRawRequest()) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestFromRaw(parseRequest()));
		} else {
			call(createAuthzenApiRequestSequence());
		}
	}

	protected void addAuthenticationToAuthzenApiEndpoint() {
		if (skipAuthentication()) {
			if (pdpAuthUsesMtls) {
				callAndStopOnFailure(RemoveMTLSCertificates.class, "AUTHZEN-11.3");
			}
			return;
		}
		if (corruptAuthCredentials()) {
			if (pdpAuthUsesMtls) {
				callAndStopOnFailure(GenerateUntrustedAuthzenMtlsClientCertificate.class, "AUTHZEN-11.3");
				env.mapKey("mutual_tls_authentication", "untrusted_mutual_tls_authentication");
			} else {
				callAndStopOnFailure(CorruptAuthzenClientCredentials.class);
			}
		}
		if (addPDPEndpointClientAuthentication != null) {
			call(sequence(addPDPEndpointClientAuthentication));
		}
	}

	protected abstract void setAuthzenApiEndpoint();

	/**
	 * Runs the Section 4 v1-path SHOULD check on the endpoint in {@code authzen_api_endpoint} the
	 * first time the module calls it, so idempotency and pagination loops do not repeat the
	 * warning, while a module that calls a second API also has that endpoint checked.
	 */
	protected void checkEndpointPathContainsV1() {
		if (endpointPathsV1Checked.add(env.getString("authzen_api_endpoint"))) {
			callAndContinueOnFailure(EnsureAuthzenApiEndpointPathContainsV1.class, ConditionResult.WARNING, "AUTHZEN-4");
		}
	}

	/**
	 * The PDP metadata parameter (Section 9.1.1) that names the endpoint of the API this module
	 * exercises.
	 */
	protected abstract String getEndpointMetadataParameter();

	/**
	 * The PDP metadata parameters naming the endpoints of any other API this module calls. Under
	 * discovery each must be advertised, as {@link #getEndpointMetadataParameter()} must.
	 */
	protected List<String> getOtherEndpointMetadataParameters() {
		return List.of();
	}

	protected JsonObject parseRequest() {
		return JsonParser.parseString(getPayload()).getAsJsonObject();
	}

	protected abstract String getPayload();
	protected abstract ConditionSequence createAuthzenApiRequestSequence();

	@Override
	protected void processCallback() {
		// Do nothing
	}

	protected void performPostApiFlow() {
		onPostAuthorizationFlowComplete();
	}

	protected void onPostAuthorizationFlowComplete() {
		fireTestFinished();
	}
}
