package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.AddActionToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddApiKeyAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest;
import net.openid.conformance.authzen.condition.AddContextToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddResourceToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddSubjectToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.authzen.condition.CheckPDPServerConfiguration;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestAction;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestContext;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestResource;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestSubject;
import net.openid.conformance.authzen.condition.CreateEmptyAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseTrue;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.GetPDPDynamicServerConfiguration;
import net.openid.conformance.authzen.condition.GetPDPStaticServerConfiguration;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationEndpoint;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
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
		addPDPEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
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
		call(sequence(CallAuthzenApiEndpointAndVerifySuccessfulResponse.class));
	}

	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-5.5");
	}

	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseTrue.class, ConditionResult.FAILURE);
	}

	public static class CreateAuthzenApiRequestSteps extends AbstractConditionSequence {
		private JsonObject subject;
		private JsonObject resource;
		private JsonObject action;
		private JsonObject context;
		CreateAuthzenApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject action, JsonObject context) {
			this.subject = subject;
			this.resource = resource;
			this.action = action;
			this.context = context;
		}
		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSubject(subject), "AUTHZEN-5.1");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestResource(resource), "AUTHZEN-5.2");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestAction(action), "AUTHZEN-5.3");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4");

			callAndStopOnFailure(AddSubjectToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndStopOnFailure(AddResourceToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndStopOnFailure(AddActionToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndContinueOnFailure(AddContextToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
		}
	}

	protected void createAuthzenApiRequest() {
		call(createAuthzenApiRequestSequence());
		if (addPDPEndpointClientAuthentication != null) {
			call(sequence(addPDPEndpointClientAuthentication));
		}
		callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationEndpoint.class);

	}

	protected abstract JsonObject parseRequest();
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenApiRequestSteps(request.getAsJsonObject("subject"), request.getAsJsonObject("resource"), request.getAsJsonObject("action"), request.getAsJsonObject("context"));
	}

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
