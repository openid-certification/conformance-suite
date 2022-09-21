package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vci-test-plan",
	displayName = "OpenID For Verifiable Credential Issuance: Test Plan (alpha)",
	profile = TestPlan.ProfileNames.optest
)
public class OID4VCITestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					OID4VCIHappyFlow.class
				),
				List.of()
			)
		);
	}
}
