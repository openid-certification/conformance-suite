package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.condition.common.GrantManagementSupport;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.GrantManagement;
import net.openid.conformance.variant.VariantNotApplicable;

/**
 * Sends an authorization request with grant_management_action=update and a fabricated invalid grant_id.
 * Expects the AS to return an invalid_grant_id error, either from the PAR endpoint or the authorization endpoint.
 */
@PublishTestModule(
	testName = "fapi2-security-profile-final-grant-management-ensure-invalid-grant-id-fails",
	displayName = "FAPI2-Security-Profile-Final: Grant Management - Ensure Invalid grant_id Fails",
	summary = "Sends an authorization request with grant_management_action=merge and a fabricated invalid grant_id. Expects the authorization server to reject the request with an invalid_grant_id error (either from the PAR endpoint or the authorization endpoint callback).",
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
public class FAPI2SPFinalGrantManagementEnsureInvalidGrantIdFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "GM-4.2");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		// Build the invalid grant_id into the environment before the request is created
		env.putString(GrantManagementSupport.GRANT_ID_KEY, "invalid-grant-id-that-does-not-exist-" + System.currentTimeMillis());

		ConditionSequence seq = super.makeCreateAuthorizationRequestSteps(usePkce);
		// Replace create with update, then add the invalid grant_id
		seq = seq.replace(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest.class,
			condition(GrantManagementSupport.AddGrantManagementActionMergeToAuthorizationRequest.class));
		seq.then(condition(GrantManagementSupport.AddGrantIdToAuthorizationRequest.class));
		return seq;
	}

	@Override
	protected void processParErrorResponse() {
		// Error came from PAR endpoint
		call(exec().mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "PAR-2.3");
		callAndContinueOnFailure(GrantManagementSupport.EnsurePAREndpointRejectsInvalidGrantId.class, Condition.ConditionResult.FAILURE, "GM-4.2");
		call(exec().unmapKey("endpoint_response"));
	}

	@Override
	protected void processCallback() {
		// Error came from authorization endpoint as redirect
		eventLog.startBlock("Verify invalid_grant_id error in authorization endpoint response");
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(GrantManagementSupport.EnsureAuthorizationEndpointRejectsInvalidGrantId.class, Condition.ConditionResult.FAILURE, "GM-4.2");
		eventLog.endBlock();
		fireTestFinished();
	}
}
