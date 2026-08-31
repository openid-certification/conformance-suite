package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Sends an authorization request with a fabricated invalid grant_id, using whichever of merge/replace the
 * authorization server advertises. Expects the AS to return an invalid_grant_id error, either from the PAR
 * endpoint or the authorization endpoint.
 *
 * <p>The action itself is not the subject of the test, but GM 5.4 only mandates invalid_grant_id for an
 * unknown grant_id - it mandates invalid_request when the AS does not support the requested action, and GM
 * 7.1 makes both merge and replace optional. The test therefore picks an advertised action and skips when
 * the AS supports neither, rather than failing a conformant server.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-ensure-invalid-grant-id-fails",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Ensure Invalid grant_id Fails",
	summary = "Sends an authorization request with a fabricated invalid grant_id, using whichever of grant_management_action=merge or replace the authorization server advertises. Expects the authorization server to reject the request with an invalid_grant_id error (either from the PAR endpoint or the authorization endpoint callback). The test is skipped if the authorization server supports neither merge nor replace, as both are OPTIONAL and there is then no request that can carry a grant_id.",
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
// Grant management needs an authorization flow that yields a grant, so it does not apply to the client
// credentials grant profile. This module cannot extend AbstractFAPI2SPFinalGrantManagementTestModule
// because it needs the PAR base class, so the exclusion is declared inline here.
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {"consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "ksa",
	"fapi_client_credentials_grant", "vci", "vci_haip"})
public class FAPI2SPFinalGrantManagementEnsureInvalidGrantIdFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "GM-5.4");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		// Build the invalid grant_id into the environment before the request is created
		callAndStopOnFailure(GrantManagementSupport.CreateGrantIdThatDoesNotExist.class, "GM-5.5");

		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		// Replace the create action with whichever grant_id-carrying action the AS advertises, then add the
		// invalid grant_id
		seq = seq.replace(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest.class,
			condition(GrantManagementSupport.SelectGrantManagementActionTakingAGrantId.class).requirement("GM-7.1"));
		seq.then(condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest.class).requirement("GM-5.2"));
		return seq;
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		// SelectGrantManagementActionTakingAGrantId leaves this unset when the AS advertises neither merge
		// nor replace. There is then no way to provoke invalid_grant_id (GM 5.4 requires invalid_request for
		// an unsupported action), so the test is skipped rather than failed.
		if (env.getString(GrantManagementSupport.SELECTED_GRANT_MANAGEMENT_ACTION_KEY) == null) {
			fireTestSkipped("The authorization server does not advertise 'merge' or 'replace' in"
				+ " grant_management_actions_supported. Both are OPTIONAL per GM 7.1, and without one of them"
				+ " there is no authorization request that carries a grant_id, so an invalid grant_id cannot"
				+ " be presented to the authorization server.");
		}
	}

	@Override
	protected void processParErrorResponse() {
		// Error came from PAR endpoint
		call(exec().mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "PAR-2.3");
		callAndContinueOnFailure(GrantManagementSupport.EnsurePAREndpointRejectsInvalidGrantId.class, Condition.ConditionResult.FAILURE, "GM-5.4");
		call(exec().unmapKey("endpoint_response"));
	}

	@Override
	protected void processCallback() {
		// Error came from authorization endpoint as redirect
		eventLog.startBlock("Verify invalid_grant_id error in authorization endpoint response");
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(GrantManagementSupport.EnsureAuthorizationEndpointRejectsInvalidGrantId.class, Condition.ConditionResult.FAILURE, "GM-5.4");
		eventLog.endBlock();
		fireTestFinished();
	}
}
