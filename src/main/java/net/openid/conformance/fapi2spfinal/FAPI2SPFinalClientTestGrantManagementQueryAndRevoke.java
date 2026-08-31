package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.testmodule.PublishTestModule;

/**
 * Client test for the grant management API (GM 6): the client creates a grant, then queries it and
 * revokes it through the grant management endpoint the suite advertises.
 *
 * <p>The test ends once the grant has been revoked, so a client that only queries - or only revokes -
 * leaves the test waiting rather than passing.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-grant-management-query-and-revoke",
	displayName = "FAPI2-Security-Profile-Final: client test for querying and revoking a grant",
	summary = "Configure your client to send a PAR request with grant_management_action=create, and to then use the grant management API. After the authorization flow the client should query the grant (HTTP GET to the grant_management_endpoint with the issued grant_id, using its access token, which will return the granted scopes) and then revoke it (HTTP DELETE to the same URL, which will return 204). The test finishes once the grant has been revoked; it will fail if the client calls the endpoint for a grant it was not issued, sends a body, or does not present a correctly sender constrained access token.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)
public class FAPI2SPFinalClientTestGrantManagementQueryAndRevoke extends AbstractFAPI2SPFinalClientTestGrantManagement {

	@Override
	protected void addCustomValuesToIdToken() {
		// Do nothing
	}

	/**
	 * The resource request is not the end of this test - the grant management calls come after it, so we
	 * simply go back to waiting for the client.
	 *
	 * <p>Deliberately not {@code startWaitingForTimeout()}: that sets {@code startingShutdown}, which the
	 * grant management endpoint treats as the client wrongly continuing to talk to the server, and it
	 * schedules a finish that would end the test before the client got to query and revoke.
	 */
	@Override
	protected void resourceEndpointCallComplete() {
		setStatus(Status.WAITING);
	}

	@Override
	protected void onGrantManagementRequestComplete() {
		if (grantRevoked) {
			if (!grantQueried) {
				eventLog.log(getName(), "The client revoked the grant without querying it first; the query is "
					+ "part of this test, so it was expected before the revocation");
			}
			finishGrantManagementTest();
		}
	}
}
