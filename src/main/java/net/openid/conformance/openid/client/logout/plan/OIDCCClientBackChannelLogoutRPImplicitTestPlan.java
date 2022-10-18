package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutAlgNone;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutNoEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWithNonce;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAlg;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAud;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongIssuer;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-back-channel-logout-rp-implicit",
	displayName = "OpenID Connect Core: Back Channel Logout RP Certification Profile Relying Party Tests (Implicit)",
	profile = TestPlan.ProfileNames.rplogouttest
)
public class OIDCCClientBackChannelLogoutRPImplicitTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		List<Class<? extends TestModule>> testModules = List.of(
			OIDCCClientTestBackChannelLogout.class,
			OIDCCClientTestBackChannelLogoutAlgNone.class,
			OIDCCClientTestBackChannelLogoutNoEvent.class,
			OIDCCClientTestBackChannelLogoutWithNonce.class,
			OIDCCClientTestBackChannelLogoutWrongAlg.class,
			OIDCCClientTestBackChannelLogoutWrongAud.class,
			OIDCCClientTestBackChannelLogoutWrongEvent.class,
			OIDCCClientTestBackChannelLogoutWrongIssuer.class
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

	public static String certificationProfileName(VariantSelection variant) {
		return "Back-Channel RP";
	}

}
