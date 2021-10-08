package net.openid.conformance.openbanking_brasil.testmodules.channels;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.channels.BankingAgentsChannelValidator;
import net.openid.conformance.openbanking_brasil.channels.BranchesChannelsValidator;
import net.openid.conformance.openbanking_brasil.channels.ElectronicChannelsValidator;
import net.openid.conformance.openbanking_brasil.channels.PhoneChannelsValidator;
import net.openid.conformance.openbanking_brasil.channels.SharedAutomatedTellerMachinesValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ChannelsApi-test",
	displayName = "Validate structure of all Channels data Api resources",
	summary = "Validate structure of all Channels data Api resources",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.consentUrl"
	}
)
public class ChannelsApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Channels Banking Agents response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "banking-agents");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BankingAgentsChannelValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Channels Branches response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "branches");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BranchesChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate Channels Electronic Channels response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "electronic-channels");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(ElectronicChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Channels Phone Channels response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "phone-channels");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PhoneChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate Channels Shared Automated Teller Machines response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "shared-automated-teller-machines");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(SharedAutomatedTellerMachinesValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
