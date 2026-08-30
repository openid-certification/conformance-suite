package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * Creates a grant, revokes it, then attempts to query it — expecting HTTP 404.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-ensure-query-after-revoke-fails",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Ensure Query After Revoke Returns 404",
	summary = "Creates a grant via a standard authorization flow, revokes it using the grant management endpoint (expecting HTTP 204), then queries the same grant (expecting HTTP 404).",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalGrantManagementEnsureQueryAfterRevokeFails extends AbstractFAPI2SPFinalGrantManagementTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Grant Management: revoke then query");

		callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class, "GM-6.3");

		// Revoke the grant
		callGrantManagementRevoke();
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeSucceeded.class, Condition.ConditionResult.FAILURE, "GM-6.5");
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeResponseBodyIsEmpty.class, Condition.ConditionResult.FAILURE, "GM-6.5");

		// Now query — expect 404
		callGrantManagementQuery();
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns404.class, Condition.ConditionResult.FAILURE, "GM-6.6");

		eventLog.endBlock();
		fireTestFinished();
	}
}
