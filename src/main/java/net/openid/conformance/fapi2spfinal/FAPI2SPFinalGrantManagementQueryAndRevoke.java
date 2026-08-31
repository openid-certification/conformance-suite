package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * Exercises the grant management API (GM 6) end to end: query the grant that the authorization flow
 * created, revoke it, then query it again and require a 404.
 *
 * <p>The create half of grant management - {@code grant_management_action=create} in the
 * authorization request and {@code grant_id} in the token response - is not what this module is for:
 * every FAPI2 OP module already covers that when {@code grant_management=enabled}, because the
 * conditions live in {@link AbstractFAPI2SPFinalServerTestModule}. What is unique here is the call
 * to the grant management endpoint itself, which is why this module is named for query and revoke
 * rather than for a happy flow.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-query-and-revoke",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Query and Revoke",
	summary = "Performs a full authorization flow with grant_management_action=create, extracts the grant_id from the token response, queries the grant (expecting HTTP 200), revokes it (expecting HTTP 204), then queries again expecting HTTP 404.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalGrantManagementQueryAndRevoke extends AbstractFAPI2SPFinalGrantManagementTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Grant Management: query, revoke, and query again");

		callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class, "GM-6.3");

		// Query the grant
		callGrantManagementQuery();
		validateGrantManagementQueryResponse();

		// Revoke the grant
		callGrantManagementRevoke();
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeSucceeded.class, Condition.ConditionResult.FAILURE, "GM-6.5");
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeResponseBodyIsEmpty.class, Condition.ConditionResult.FAILURE, "GM-6.5");

		// Query after revoke — must return 404
		callGrantManagementQuery();
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns404.class, Condition.ConditionResult.FAILURE, "GM-6.6");

		eventLog.endBlock();

		fireTestFinished();
	}
}
