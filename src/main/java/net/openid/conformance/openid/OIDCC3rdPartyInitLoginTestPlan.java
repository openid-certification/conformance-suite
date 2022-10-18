package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-3rdparty-init-login-certification-test-plan",
	displayName = "OpenID Connect Core: 3rd party initiated login Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCC3rdPartyInitLoginTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match 'Third Party-Initiated Login OP Profile' as defined here:
		// https://openid.net/certification/testing/

		final List<Variant> variantCodeBasic = List.of(
			new Variant(ServerMetadata.class, "discovery"),
			new Variant(ClientRegistration.class, "dynamic_client"),
			// the choice of client_secret_basic here is relatively arbitary, and client_secret_post could have been
			// used instead - the certification profile requires that both basic and post are tested, but doesn't
			// dictate which variant the other tests are run with
			new Variant(ClientAuthType.class, "client_secret_basic"),
			new Variant(ResponseMode.class, "default")

		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCC3rdPartyInitLogin.class, // OP-3rd_party-init-login
					OIDCC3rdPartyInitLoginNonHttps.class// OP-3rd_party-init-login-nohttps
				),
				variantCodeBasic
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "3rd Party-Init OP";
	}
}
