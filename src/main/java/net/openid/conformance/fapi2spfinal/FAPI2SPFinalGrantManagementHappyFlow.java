package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

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
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
public class FAPI2SPFinalGrantManagementHappyFlow extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Grant Management: query, revoke, and query again");

		callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class);

		// Query the grant
		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointQuery.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementQuerySucceeded.class, Condition.ConditionResult.FAILURE, "GM-4.1");
		callAndContinueOnFailure(GrantManagementSupport.ValidateGrantManagementQueryResponse.class, Condition.ConditionResult.FAILURE, "GM-4.1");

		// Revoke the grant
		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointRevoke.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementRevokeSucceeded.class, Condition.ConditionResult.FAILURE, "GM-4.3");

		// Query after revoke — must return 404
		callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointFailureQuery.class);
		callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns404.class, Condition.ConditionResult.FAILURE, "GM-4.1");

		eventLog.endBlock();

		fireTestFinished();
	}
}
