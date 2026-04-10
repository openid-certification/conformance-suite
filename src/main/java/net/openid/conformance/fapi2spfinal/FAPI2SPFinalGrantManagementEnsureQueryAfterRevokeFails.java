package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

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
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
public class FAPI2SPFinalGrantManagementEnsureQueryAfterRevokeFails extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Grant Management: revoke then query");

		callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class);

		// Revoke the grant
		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointRevoke.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeSucceeded.class, Condition.ConditionResult.FAILURE, "GM-4.3");

		// Now query — expect 404
		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointFailureQuery.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns404.class, Condition.ConditionResult.FAILURE, "GM-4.1");

		eventLog.endBlock();
		fireTestFinished();
	}
}
