package net.openid.conformance.openid.client.logout.plan;

import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutAlgNone;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutNoEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWithNonce;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAlg;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongAud;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongEvent;
import net.openid.conformance.openid.client.logout.OIDCCClientTestBackChannelLogoutWrongIssuer;
import net.openid.conformance.openid.client.logout.OIDCCClientTestFrontChannelLogoutOPInitiated;
import net.openid.conformance.openid.client.logout.OIDCCClientTestFrontChannelLogoutRPInitiated;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogout;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutInvalidState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestRPInitLogoutNoState;
import net.openid.conformance.openid.client.logout.OIDCCClientTestSessionManagement;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "oidcc-client-logout-test-plan",
	displayName = "OpenID Connect Core Client Logout Tests: Relying party logout tests",
	profile = TestPlan.ProfileNames.rplogouttest,
	testModules = {
		OIDCCClientTestSessionManagement.class,
		OIDCCClientTestRPInitLogout.class,
		OIDCCClientTestRPInitLogoutInvalidState.class,
		OIDCCClientTestRPInitLogoutNoState.class,
		OIDCCClientTestFrontChannelLogoutRPInitiated.class,
		OIDCCClientTestFrontChannelLogoutOPInitiated.class,
		OIDCCClientTestBackChannelLogout.class,
		OIDCCClientTestBackChannelLogoutAlgNone.class,
		OIDCCClientTestBackChannelLogoutNoEvent.class,
		OIDCCClientTestBackChannelLogoutWithNonce.class,
		OIDCCClientTestBackChannelLogoutWrongAlg.class,
		OIDCCClientTestBackChannelLogoutWrongAud.class,
		OIDCCClientTestBackChannelLogoutWrongEvent.class,
		OIDCCClientTestBackChannelLogoutWrongIssuer.class
	}
)
public class OIDCCClientLogoutTestPlan implements TestPlan {
}
