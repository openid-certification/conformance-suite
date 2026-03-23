package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-initiated-logout-rp-implicit",
	displayName = "OpenID Connect Core: RP Initiated Logout RP Certification Profile Relying Party Tests (Implicit)",
	profile = TestPlan.ProfileNames.rplogouttest,
	specFamily = TestPlan.SpecFamilyNames.oidccLogout
)
public class OIDCCClientRPInitiatedLogoutRPImplicitTestPlan implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		List<Class<? extends TestModule>> testModules = List.of(
			OIDCCClientTestRPInitLogout.class,
			OIDCCClientTestRPInitLogoutInvalidState.class,
			OIDCCClientTestRPInitLogoutNoState.class
		);
		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(new Variant(ResponseType.class, "id_token"))
			),
			new ModuleListEntry(
				testModules,
				List.of(new Variant(ResponseType.class, "id_token token"))
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("RP-Initiated RP");
	}

}
