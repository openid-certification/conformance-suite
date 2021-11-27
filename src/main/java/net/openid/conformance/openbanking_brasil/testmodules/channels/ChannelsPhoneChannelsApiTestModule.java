package net.openid.conformance.openbanking_brasil.testmodules.channels;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.channels.PhoneChannelsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "Channels Phone Channels Api test",
	displayName = "Validate structure of Channels Phone Channels Api resources",
	summary = "Validate structure of Channels Phone Channels Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1,
	configurationFields = {
		"server.discoveryUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.consentUrl"
	}
)
public class ChannelsPhoneChannelsApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Channels Phone Channels response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "phone-channels");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PhoneChannelsValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
