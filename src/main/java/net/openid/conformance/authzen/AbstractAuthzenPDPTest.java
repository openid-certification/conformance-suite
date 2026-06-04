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
import net.openid.conformance.authzen.condition.CheckPDPServerConfiguration;
import net.openid.conformance.authzen.condition.CorruptAuthzenClientCredentials;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestFromRaw;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataParamsNotEmpty;
import net.openid.conformance.authzen.condition.EnsureDiscoveryMetadataResponseValid;
import net.openid.conformance.authzen.condition.EnsureMetadataCapabilitiesValid;
import net.openid.conformance.authzen.condition.EnsurePolicyDecisionPointMatchesIssuer;
import net.openid.conformance.authzen.condition.GetPDPDynamicServerConfiguration;
import net.openid.conformance.authzen.condition.GetPDPStaticServerConfiguration;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
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
	"pdp.policy_decision_point",
	"pdp.access_evaluation_endpoint"
})
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "discovery", configurationFields = {
	"pdp.policy_decision_point"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_id",
	"client.client_secret"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "api_key", configurationFields = {
	"pdp.api_key"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca"
})


public abstract class AbstractAuthzenPDPTest extends AbstractRedirectServerTestModule {
	protected boolean serverSupportsDiscovery;
	protected Class<? extends ConditionSequence> profileCompleteClientConfiguration;
	protected Class<? extends ConditionSequence> addPDPEndpointClientAuthentication;
	protected Class<? extends ConditionSequence> supportMTLSEndpointAliases;

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
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "api_key")
	public void setupApiKey() {
		profileCompleteClientConfiguration = null;
		addPDPEndpointClientAuthentication = AddApiKeyAuthenticationToAuthzenApiRequest.class;
	}

	@VariantSetup(parameter = PDPAuthType.class, value = "mtls")
	public void setupMtls() {
		profileCompleteClientConfiguration = ConfigureClientForMtls.class;
		addPDPEndpointClientAuthentication = AddMTLSClientAuthenticationToRequest.class;
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

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
		serverSupportsDiscovery = getVariant(PDPServerMetadata.class) == PDPServerMetadata.DISCOVERY;

		PDPAuthType PdpAuthType = getVariant(PDPAuthType.class);
		env.putString("client_auth_type", PdpAuthType.toString());

		switch (getVariant(PDPServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(GetPDPDynamicServerConfiguration.class);
				callAndContinueOnFailure(EnsureDiscoveryMetadataResponseValid.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
				break;
			case STATIC:
				callAndStopOnFailure(GetPDPStaticServerConfiguration.class);
				break;
		}

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		env.mapKey("server", "pdp");
		callAndContinueOnFailure(CheckPDPServerConfiguration.class, ConditionResult.FAILURE, "AUTHZEN-9.1.1");
		if (serverSupportsDiscovery) {
			callAndContinueOnFailure(EnsurePolicyDecisionPointMatchesIssuer.class, ConditionResult.FAILURE, "AUTHZEN-9.2.3");
			callAndContinueOnFailure(EnsureDiscoveryMetadataParamsNotEmpty.class, ConditionResult.FAILURE, "AUTHZEN-9.2.2");
		}
		callAndContinueOnFailure(EnsureMetadataCapabilitiesValid.class, ConditionResult.FAILURE, "AUTHZEN-9.1.2");
		env.unmapKey("server");

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
		if (getAcceptableHttpStatusCodes().contains(200)) {
			processAuthApiEndpointResponse();
			validateAuthApiEndpointResponse();
		}
		performPostApiFlow();
		eventLog.endBlock();
	}

	/**
	 * Apply request overrides, add the X-Request-ID header when enabled, dispatch
	 * the API call, and assert the X-Request-ID echo. Override the per-iteration
	 * loop in {@link AbstractAuthzenPDPEvaluationsIdempotencyTest} and friends to
	 * pick up these per-request behaviors.
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
	 * Override to add an `X-Request-ID` header to the request. When true, the request
	 * sequence appends the header and the response is asserted to echo the same value
	 * (Section 10.1.3).
	 */
	protected boolean includeXRequestIdHeader() {
		return false;
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
		callAndContinueOnFailure(EnsureAuthzenApiEndpointPathContainsV1.class, ConditionResult.WARNING, "AUTHZEN-4");
		addAuthenticationToAuthzenApiEndpoint();
		performApiRequestCall();
	}

	protected void performApiRequestCall() {
		Set<Integer> acceptable = getAcceptableHttpStatusCodes();
		if (acceptable.size() == 1 && acceptable.iterator().next() == 200) {
			call(sequence(CallAuthzenApiEndpointAndVerifySuccessfulResponse.class));
		} else {
			JsonObject wrapper = new JsonObject();
			JsonArray codes = new JsonArray();
			for (Integer code : acceptable) {
				codes.add(code);
			}
			wrapper.add("codes", codes);
			env.putObject("authzen_expected_http_status_codes", wrapper);
			call(sequence(CallAuthzenApiEndpointAndVerifyExpectedStatus.class));
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
