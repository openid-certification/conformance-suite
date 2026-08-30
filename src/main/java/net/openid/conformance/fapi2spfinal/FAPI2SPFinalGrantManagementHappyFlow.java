package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-happy-flow",
	displayName = "FAPI2-Security-Profile-Final: Grant Management Happy Flow",
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
public class FAPI2SPFinalGrantManagementHappyFlow extends AbstractFAPI2SPFinalGrantManagementTestModule {

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
