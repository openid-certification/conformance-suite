package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Client 1 creates a grant. Client 2 then tries to query the same grant using its own access token.
 * Expects HTTP 403 or 404.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-ensure-wrong-client-cannot-query-grant",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Ensure Wrong Client Cannot Query Grant",
	summary = "Client 1 performs a full authorization flow and creates a grant. Client 2 then performs its own authorization flow to obtain an access token, and then attempts to query Client 1's grant using its own access token. The test expects the grant management endpoint to return HTTP 403 or 404.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
public class FAPI2SPFinalGrantManagementEnsureWrongClientCannotQueryGrant extends AbstractFAPI2SPFinalMultipleClient {

	private String client1GrantId;

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			// Save client1's grant_id before the second auth flow overwrites it
			client1GrantId = env.getString(GrantManagementSupport.GRANT_ID_KEY);

			// Run the second client flow
			performAuthorizationFlowWithSecondClient();
		} else {
			// Client 2 is now authenticated, has its own access token mapped
			// Restore client1's grant_id and build the GM URL
			env.putString(GrantManagementSupport.GRANT_ID_KEY, client1GrantId);
			callAndStopOnFailure(GrantManagementSupport.SetGrantManagementEndpointUrl.class);

			// Client 2 uses its access token to query client 1's grant — must fail
			eventLog.startBlock("Client 2 attempts to query Client 1's grant");
			callAndStopOnFailure(GrantManagementSupport.CallGrantManagementEndpointFailureQuery.class);
			callAndContinueOnFailure(GrantManagementSupport.EnsureGrantManagementEndpointReturns403Or404.class, Condition.ConditionResult.FAILURE, "GM-4.1");
			eventLog.endBlock();

			fireTestFinished();
		}
	}
}
