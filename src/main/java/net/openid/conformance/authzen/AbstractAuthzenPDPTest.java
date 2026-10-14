package net.openid.conformance.authzen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;
import net.openid.conformance.authzen.condition.AddApiKeyAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddXRequestIdHeaderToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifyExpectedStatus;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiEndpointPathContainsV1;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiResponseXRequestIdMatches;
import net.openid.conformance.authzen.condition.EnsurePDPMetadataAdvertisesEndpoint;
import net.openid.conformance.authzen.condition.CorruptAuthzenClientCredentials;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestFromRaw;
import net.openid.conformance.authzen.condition.GetAuthzenClientConfiguration;
import net.openid.conformance.authzen.condition.GetPDPStaticServerConfiguration;
import net.openid.conformance.authzen.condition.ValidatePDPIdentifier;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
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
	protected Class<? extends Condition> loadClientConfiguration;
	private boolean endpointPathV1Checked;

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
			callAndStopOnFailure(new EnsurePDPMetadataAdvertisesEndpoint(getEndpointMetadataParameter()), "AUTHZEN-9.1.1");
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
		if (includeXRequestIdHeader()) {
			callAndContinueOnFailure(EnsureAuthzenApiResponseXRequestIdMatches.class, ConditionResult.FAILURE, "AUTHZEN-10.1.3");
		}
	}

	/**
	 * Standard 3-iteration idempotency loop used by every API family
	 * (Evaluation / Evaluations / Subject Search / Resource Search / Action Search).
	 * Each idempotency abstract base delegates to this method, passing its own
	 * API-family label and the capture/match condition classes appropriate for
	 * that family (decision, batch decisions or search results). The loop opens nested eventLog
	 * blocks (one outer + one per iteration), each wrapped in try/finally so
	 * block scopes always close even when a condition throws.
	 */
	protected void runIdempotencyLoop(String apiFamilyLabel,
			Class<? extends Condition> capture,
			Class<? extends Condition> match) {
		final int iterations = 3;
		eventLog.startBlock("Idempotency test: send the same " + apiFamilyLabel + " request " + iterations + " times");
		try {
			for (int i = 1; i <= iterations; i++) {
				eventLog.startBlock("Iteration " + i);
				try {
					resetStateBetweenIdenticalRequests();
					createAuthzenApiRequest();
					performSingleApiRequest();
					processAuthApiEndpointResponse();
					validateAuthApiEndpointResponse();
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
		} finally {
			eventLog.endBlock();
		}
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
		if (!endpointPathV1Checked) {
			// The endpoint URL is fixed per test, so the §4 v1-path SHOULD check
			// only needs to fire once. Without this gate, idempotency loops (×3)
			// and paginated search loops (up to MAX_PAGES) would emit the same
			// warning every iteration.
			callAndContinueOnFailure(EnsureAuthzenApiEndpointPathContainsV1.class, ConditionResult.WARNING, "AUTHZEN-4");
			endpointPathV1Checked = true;
		}
		addAuthenticationToAuthzenApiEndpoint();
		performApiRequestCall();
	}

	protected void performApiRequestCall() {
		Set<Integer> acceptable = getAcceptableHttpStatusCodes();
		if (acceptable.size() == 1 && acceptable.iterator().next().intValue() == 200) {
			call(sequence(CallAuthzenApiEndpointAndVerifySuccessfulResponse.class));
		} else {
			JsonObject wrapper = new JsonObject();
			JsonArray codes = new JsonArray();
			for (Integer code : acceptable) {
				codes.add(code);
			}
			wrapper.add("codes", codes);
			env.putObject("authzen_expected_http_status_codes", wrapper);
			call(new CallAuthzenApiEndpointAndVerifyExpectedStatus(expectedStatusRequirements(acceptable)));
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
	 * exact-code tests keep working. Negative transport tests where the spec only requires
	 * "some 4xx" (§10.1.1/§10.1.2) should return e.g. {@code Set.of(400, 405)} for method
	 * rejection or {@code Set.of(400, 415)} for content-type rejection.
	 */
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return Set.of(getExpectedHttpStatusCode());
	}

	/**
	 * The section requiring the status a module expects: the HTTPS POST binding for a request with
	 * another method, Section 11.3 for 401, Section 10.1.1 for the 400 a malformed request gets,
	 * otherwise the HTTPS binding.
	 */
	private String[] expectedStatusRequirements(Set<Integer> acceptable) {
		if (!"POST".equals(getRequestHttpMethod())) {
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
			return;
		}
		if (corruptAuthCredentials()) {
			callAndStopOnFailure(CorruptAuthzenClientCredentials.class);
		}
		if (addPDPEndpointClientAuthentication != null) {
			mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
			call(sequence(addPDPEndpointClientAuthentication));
			unmapClientAuthKeys();
		}
	}

	protected abstract void setAuthzenApiEndpoint();

	/**
	 * The PDP metadata parameter (Section 9.1.1) that names the endpoint of the API this module
	 * exercises.
	 */
	protected abstract String getEndpointMetadataParameter();

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
