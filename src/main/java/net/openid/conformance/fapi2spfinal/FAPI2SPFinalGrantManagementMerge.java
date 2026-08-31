package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

/**
 * Tests grant_management_action=merge: first auth flow creates a grant, second reuses it with merge action.
 * After the second token response, queries the grant to verify it is still valid.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-merge",
	displayName = "FAPI2-Security-Profile-Final: Grant Management Merge",
	summary = "Performs two authorization flows. The first uses grant_management_action=create to create a new grant. The second uses grant_management_action=merge with the previously issued grant_id. After the second token response, queries the grant endpoint to verify the grant is valid.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalGrantManagementMerge extends AbstractFAPI2SPFinalGrantManagementTestModule {

	private boolean secondAuthFlowDone = false;

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		if (secondAuthFlowDone) {
			// Replace the create action with merge and add the grant_id
			seq = seq.replace(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest.class,
				condition(GrantManagementSupport.AddGrantManagementActionMergeToAuthorizationRequest.class).requirement("GM-5.2"));
			seq.then(condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest.class).requirement("GM-5.2"));
		}
		return seq;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!secondAuthFlowDone) {
			// the generic discovery check cannot require merge of every server, so check it here, where the
			// test is about to depend on it
			skipIfElementMissing("server", "grant_management_actions_supported", Condition.ConditionResult.INFO,
				GrantManagementSupport.CheckGrantManagementActionsSupportedContainsMerge.class, Condition.ConditionResult.FAILURE, "GM-7.1");

			// keep the issued grant_id, so the second flow can be checked against it once the AS has
			// answered with a grant_id of its own
			callAndStopOnFailure(GrantManagementSupport.StoreGrantIdForComparison.class, "GM-5.5");

			// First flow done: trigger second flow with merge
			secondAuthFlowDone = true;
			performAuthorizationFlow();
		} else {
			// Second flow done: query the grant
			eventLog.startBlock("Grant Management: query after merge");
			// a 'merge' updates the grant the request named, so the AS must not have minted a new one
			callAndContinueOnFailure(GrantManagementSupport.EnsureGrantIdIsUnchanged.class, Condition.ConditionResult.WARNING, "GM-5.2");
			callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class, "GM-6.3");
			callGrantManagementQuery();
			validateGrantManagementQueryResponse();
			// the grant the AS reports has to cover the access it just issued, otherwise the 'merge' did not
			// take effect
			skipIfElementMissing(GrantManagementSupport.GRANT_MANAGEMENT_RESPONSE_KEY, "body_json", Condition.ConditionResult.INFO,
				GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope.class, Condition.ConditionResult.WARNING, "GM-6.4");
			eventLog.endBlock();

			fireTestFinished();
		}
	}
}
