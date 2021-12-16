package net.openid.conformance.openbanking_brasil.testmodules.channels;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.channels.SharedAutomatedTellerMachinesValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Channels Shared Automated Teller Machines test",
	displayName = "Validate structure of Channels Shared Automated Teller Machines Api resources",
	summary = "Validate structure of Channels Shared Automated Teller Machines Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	configurationFields = {
		"server.discoveryUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.consentUrl"
	}
)
public class ChannelsSharedAutomatedTellerMachinesApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Channels Shared Automated Teller Machines response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "shared-automated-teller-machines");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(SharedAutomatedTellerMachinesValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
