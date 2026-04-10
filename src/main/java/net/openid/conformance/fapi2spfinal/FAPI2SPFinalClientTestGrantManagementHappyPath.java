package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Happy path client test with Grant Management enabled.
 * The suite (acting as AS) returns a grant_id in the token response.
 * The client should complete the flow successfully, accepting the grant_id.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-grant-management-happy-path",
	displayName = "FAPI2-Security-Profile-Final: client test for happy path with Grant Management",
	summary = "Tests a 'happy path' flow with Grant Management enabled. The suite returns a grant_id in the token response. The client should perform OpenID discovery, call the authorization endpoint, exchange the authorization code for an access token (which will include a grant_id), and make a GET request to the resource endpoint.",
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
@VariantNotApplicable(parameter = GrantManagement.class, values = {"disabled"})
public class FAPI2SPFinalClientTestGrantManagementHappyPath extends AbstractFAPI2SPFinalClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
		// Do nothing
	}
}
