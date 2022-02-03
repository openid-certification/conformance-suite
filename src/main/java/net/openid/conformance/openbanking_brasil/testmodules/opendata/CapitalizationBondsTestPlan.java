package net.openid.conformance.openbanking_brasil.testmodules.opendata;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.opendata.capitalizationBonds.CapitalizationBondsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.opendata.utils.PrepareToGetOpenDataApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Opendata - Capitalization Bonds API test",
	profile = OBBProfile.OBB_PROFIlE_PHASE4,
	displayName = "Functional tests for Opendata - Capitalization Bonds API - based on Swagger version: 1.0.0",
	summary = "Structural and logical tests for Opendata - Capitalization Bonds API"
)
public class CapitalizationBondsTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					CapitalizationBondsApiTestModule.class
				),
				List.of(
					new Variant(ClientAuthType.class, "none")
				)
			)
		);
	}

	@PublishTestModule(
		testName = "Opendata - Capitalization Bonds API test",
		displayName = "Validate structure of Opendata - Capitalization Bonds API Api resources",
		summary = "Validate structure of Opendata - Capitalization Bonds Api resources",
		profile = OBBProfile.OBB_PROFIlE_PHASE4)
	public static class CapitalizationBondsApiTestModule extends AbstractNoAuthFunctionalTestModule {

		@Override
		protected void runTests() {
			runInBlock("Validate Opendata - Capitalization Bonds response", () -> {
				callAndStopOnFailure(PrepareToGetOpenDataApi.class);
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(CapitalizationBondsValidator.class, Condition.ConditionResult.FAILURE);
			});
		}
	}
}
