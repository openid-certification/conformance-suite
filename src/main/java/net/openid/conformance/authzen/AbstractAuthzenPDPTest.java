package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.AddApiKeyAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.authzen.condition.CheckPDPServerConfiguration;
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
import net.openid.conformance.variant.PDPAuthType;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	PDPServerMetadata.class,
	PDPAuthType.class
})
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.policy_decision_point",
	"pdp.access_evaluation_endpoint",
	"pdp.access_evaluations_endpoint",
	"pdp.search_subject_endpoint",
	"pdp.search_action_endpoint",
	"pdp.search_resource_endpoint"
})
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "discovery", configurationFields = {
	"pdp.policy_decision_point"
})
@VariantConfigurationFields(parameter = PDPAuthType.class, value = "client_secret_basic", configurationFields = {
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
		callAndStopOnFailure(CheckPDPServerConfiguration.class);
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
		callAuthApiEndpointRequest();
		processAuthApiEndpointResponse();
		validateAuthApiEndpointResponse();
		performPostApiFlow();
		eventLog.endBlock();
	}

	protected void callAuthApiEndpointRequest() {
		setAuthzenApiEndpoint();
		addAuthenticationToAuthzenApiEndpoint();
		performApiRequestCall();
	}

	protected void performApiRequestCall() {
		call(sequence(CallAuthzenApiEndpointAndVerifySuccessfulResponse.class));
	}

	protected abstract void processAuthApiEndpointResponse();

	protected abstract void validateAuthApiEndpointResponse();

	protected void createAuthzenApiRequest() {
		call(createAuthzenApiRequestSequence());
	}

	protected void addAuthenticationToAuthzenApiEndpoint() {
		if (addPDPEndpointClientAuthentication != null) {
			mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
			call(sequence(addPDPEndpointClientAuthentication));
			unmapClientAuthKeys();
		}
	}

	protected abstract void setAuthzenApiEndpoint();

	protected abstract JsonObject parseRequest();
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
