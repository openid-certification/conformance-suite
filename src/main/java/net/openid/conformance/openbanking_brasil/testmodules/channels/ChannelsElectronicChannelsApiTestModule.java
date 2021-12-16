package net.openid.conformance.openbanking_brasil.testmodules.channels;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.channels.ElectronicChannelsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Channels Electronic Channels Api test",
	displayName = "Validate structure of Channels Electronic Channels Api resources",
	summary = "Validate structure of Channels Electronic Channels Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class ChannelsElectronicChannelsApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Channels Electronic Channels response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "electronic-channels");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(ElectronicChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
