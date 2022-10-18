package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-rp-initiated-logout-rp-basic",
	displayName = "OpenID Connect Core: RP Initiated Logout RP Certification Profile Relying Party Tests (Basic)",
	profile = TestPlan.ProfileNames.rplogouttest
)
public class OIDCCClientRPInitiatedLogoutRPBasicTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantResponseTypeCode = List.of(
			new Variant(ResponseType.class, "code")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestRPInitLogout.class,
					OIDCCClientTestRPInitLogoutInvalidState.class,
					OIDCCClientTestRPInitLogoutNoState.class
				),
				variantResponseTypeCode
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "RP-Initiated RP";
	}

}
