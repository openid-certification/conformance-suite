package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

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
public class FAPI2SPFinalGrantManagementReplace extends AbstractFAPI2SPFinalGrantManagementTestModule {

	private boolean secondAuthFlowDone = false;

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		if (secondAuthFlowDone) {
			// Replace the create action with replace and add the grant_id
			seq = seq.replace(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest.class,
				condition(GrantManagementSupport.AddGrantManagementActionReplaceToAuthorizationRequest.class).requirement("GM-5.2"));
			seq.then(condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest.class).requirement("GM-5.2"));
		}
		return seq;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!secondAuthFlowDone) {
			// the generic discovery check cannot require replace of every server, so check it here, where the
			// test is about to depend on it
			skipIfElementMissing("server", "grant_management_actions_supported", Condition.ConditionResult.INFO,
				GrantManagementSupport.CheckGrantManagementActionsSupportedContainsReplace.class, Condition.ConditionResult.FAILURE, "GM-7.1");

			// First flow done: trigger second flow with replace
			secondAuthFlowDone = true;
			performAuthorizationFlow();
		} else {
			// Second flow done: query the grant
			eventLog.startBlock("Grant Management: query after replace");
			callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class, "GM-6.3");
			callGrantManagementQuery();
			validateGrantManagementQueryResponse();
			eventLog.endBlock();

			fireTestFinished();
		}
	}
}
