package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Client test where the suite (acting as AS) returns invalid_grant_id from the PAR endpoint.
 * The client should send a PAR request with grant_management_action=update and a grant_id.
 * The suite rejects it with invalid_grant_id (HTTP 400), and the client must handle the error
 * gracefully without proceeding to the authorization or token endpoint.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-grant-management-invalid-grant-id-fails",
	displayName = "FAPI2-Security-Profile-Final: client test - invalid grant_id from PAR endpoint, should be rejected",
	summary = "Configure your client to send a PAR request with grant_management_action=update and a grant_id. The suite will return an invalid_grant_id error (HTTP 400) from the PAR endpoint. The test expects the client to display an error indicating that the grant_id was rejected, and to NOT proceed to the authorization endpoint.",
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
public class FAPI2SPFinalClientTestGrantManagementInvalidGrantIdFails extends AbstractFAPI2SPFinalClientTest {

	@Override
	protected ResponseEntity<Object> createPAREndpointCustomErrorResponse() {
		callAndStopOnFailure(GrantManagementSupport.CreatePAREndpointInvalidGrantIdErrorResponse.class, "GM-4.2");
		startWaitingForTimeout();
		return new ResponseEntity<>(
			env.getObject("par_endpoint_response"),
			headersFromJson(env.getObject("par_endpoint_response_headers")),
			HttpStatus.valueOf(env.getInteger("par_endpoint_response_http_status").intValue())
		);
	}

	@Override
	protected void addCustomValuesToIdToken() {
		// Never called — PAR endpoint returns an error before token issuance
	}

	@Override
	protected Object authorizationEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client incorrectly called the authorization endpoint after receiving an invalid_grant_id error from the PAR endpoint");
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client incorrectly called the token endpoint after receiving an invalid_grant_id error from the PAR endpoint");
	}
}
