package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddMultipleRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AuthorizationEndpointRedirectedBackUnexpectedly;
import net.openid.conformance.condition.client.ExpectRedirectUriMissingErrorPage;
import net.openid.conformance.condition.client.RemoveRedirectUriFromAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OP-redirect_uri-Missing
@PublishTestModule(
	testName = "oidcc-ensure-redirect-uri-in-authorization-request",
	displayName = "OIDCC: ensure redirect URI in authorization request",
	summary = "This test registers a client that has two redirect uris and sends a request without redirect_uri to authorization server - this must result in the authorization server showing an error page (a screenshot of which should be uploaded).",
	profile = "OIDCC"
)

@VariantNotApplicable(parameter = ClientRegistration.class, values={"static_client"})
public class OIDCCEnsureRedirectUriInAuthorizationRequest extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "OIDCC-3.1.2.1");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_missing_error"));
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddMultipleRedirectUriToDynamicRegistrationRequest.class);
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(RemoveRedirectUriFromAuthorizationEndpointRequest.class));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(AuthorizationEndpointRedirectedBackUnexpectedly.class, Condition.ConditionResult.FAILURE);
		fireTestFinished();
	}
}
