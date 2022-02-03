package net.openid.conformance.openbanking_brasil.testmodules.channels;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.channels.BranchesChannelsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Channels Branches Api test",
	displayName = "Validate structure of Channels Branches Api resources",
	summary = "Validate structure of Channels Branches Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class ChannelsBranchesApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Channels Branches response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(LogOnlyFailure.class);
			callAndContinueOnFailure(BranchesChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
