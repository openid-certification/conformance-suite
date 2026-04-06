package net.openid.conformance.openid;

import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpointReorderedParams;
import net.openid.conformance.condition.client.ReverseScopeOrderInAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-alternate-happy-flow",
	displayName = "OIDCC: alternate happy flow",
	summary = "This test performs a happy flow but with the order of the entries in the 'scope' reversed and authorization endpoint query parameters in a different order, to verify the server does not depend on any particular ordering. As per RFC6749 section 3.3, 'If the value contains multiple space-delimited strings, their order does not matter'.",
	profile = "OIDCC"
)
public class OIDCCAlternateHappyFlow extends OIDCCScopeEmail {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(ReverseScopeOrderInAuthorizationEndpointRequest.class).requirement("RFC6749-3.3"));
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpointReorderedParams.class);
	}

}
