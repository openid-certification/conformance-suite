package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Fabricates an unknown grant_id and attempts to query it at the grant management endpoint.
 * Expects HTTP 404.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-ensure-query-nonexistent-grant-fails",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Ensure Query of Non-Existent Grant Returns 404",
	summary = "Performs a standard authorization flow to obtain a valid access token, then attempts to query the grant management endpoint using a fabricated grant_id. Expects the server to return HTTP 404.",
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
public class FAPI2SPFinalGrantManagementEnsureQueryNonExistentGrantFails extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Grant Management: query non-existent grant");

		// Replace the real grant_id with a fabricated one
		env.putString(GrantManagementSupport.GRANT_ID_KEY, "nonexistent-grant-id-" + System.currentTimeMillis());
		callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class);

		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointFailureQuery.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns404.class, Condition.ConditionResult.FAILURE, "GM-4.1");

		eventLog.endBlock();
		fireTestFinished();
	}
}
