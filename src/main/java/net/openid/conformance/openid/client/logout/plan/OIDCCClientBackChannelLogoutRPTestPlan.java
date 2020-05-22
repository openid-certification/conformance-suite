package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutAlgNone;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutNoEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWithNonce;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAlg;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAud;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongIssuer;
import net.openid.conformance.openid.client.logout.OIDCCClientTestFrontChannelLogoutRPInitiated;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-back-channel-logout-rp",
	displayName = "OpenID Connect Core: Back Channel Logout RP Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientBackChannelLogoutRPTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {

		final List<Variant> variantSettings = List.of(
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestBackChannelLogout.class,
					OIDCCClientTestBackChannelLogoutAlgNone.class,
					OIDCCClientTestBackChannelLogoutNoEvent.class,
					OIDCCClientTestBackChannelLogoutWithNonce.class,
					OIDCCClientTestBackChannelLogoutWrongAlg.class,
					OIDCCClientTestBackChannelLogoutWrongAud.class,
					OIDCCClientTestBackChannelLogoutWrongEvent.class,
					OIDCCClientTestBackChannelLogoutWrongIssuer.class
				),
				variantSettings
			)
		);
	}
}
