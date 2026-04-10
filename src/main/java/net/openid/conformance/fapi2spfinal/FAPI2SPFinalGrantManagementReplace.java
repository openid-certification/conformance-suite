package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Tests grant_management_action=replace: first auth flow creates a grant, second reuses it with replace action.
 * After the second token response, queries the grant to verify it is valid with replaced permissions.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-replace",
	displayName = "FAPI2-Security-Profile-Final: Grant Management Replace",
	summary = "Performs two authorization flows. The first uses grant_management_action=create to create a new grant. The second uses grant_management_action=replace with the previously issued grant_id. After the second token response, queries the grant endpoint to verify the grant is valid.",
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
public class FAPI2SPFinalGrantManagementReplace extends AbstractFAPI2SPFinalServerTestModule {

	private boolean secondAuthFlowDone = false;

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		if (secondAuthFlowDone) {
			// Replace the create action with replace and add the grant_id
			seq = seq.replace(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest.class,
				condition(GrantManagementSupport.AddGrantManagementActionReplaceToAuthorizationRequest.class));
			seq.then(condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest.class));
		}
		return seq;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!secondAuthFlowDone) {
			// First flow done: trigger second flow with replace
			secondAuthFlowDone = true;
			performAuthorizationFlow();
		} else {
			// Second flow done: query the grant
			eventLog.startBlock("Grant Management: query after replace");
			callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class);
			callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointQuery.class);
			callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementQuerySucceeded.class, Condition.ConditionResult.FAILURE, "GM-4.1");
			callAndContinueOnFailure(GrantManagementSupport.ValidateGrantManagementQueryResponse.class, Condition.ConditionResult.FAILURE, "GM-4.1");
			eventLog.endBlock();

			fireTestFinished();
		}
	}
}
