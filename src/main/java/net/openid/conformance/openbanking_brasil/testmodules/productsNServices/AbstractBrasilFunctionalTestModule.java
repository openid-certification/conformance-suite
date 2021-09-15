package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractBlockLoggingTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	ClientAuthType.class,

})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "none",
	configurationFields = {
	"server.discoveryUrl",
	"resource.brazilCpf",
	"resource.resourceUrl",
	"resource.consentUrl"
})
public abstract class AbstractBrasilFunctionalTestModule extends AbstractBlockLoggingTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		runTests();
		fireTestFinished();

	}

	protected void preCallResource(String blockHeader) {
		callAndStopOnFailure(CallResource.class);

	}

	protected abstract void runTests();
}
