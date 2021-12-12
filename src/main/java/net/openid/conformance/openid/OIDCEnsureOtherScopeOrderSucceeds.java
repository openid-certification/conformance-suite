package net.openid.conformance.openid;

import net.openid.conformance.condition.client.ReverseScopeOrderInAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-ensure-other-scope-order-succeeds",
	displayName = "OIDCC: ensure other scope order succeeds",
	summary = "This test makes a scope=openid email request, but with the order of the entries in the 'scope' reversed, which must succeed. As per RFC6749 section 3.3, 'If the value contains multiple space-delimited strings, their order does not matter'. The reason for this test is that some OAuth clients process scopes in a way that the order they are sent to the server is not under the control of a developer using that client, and as per the spec such requests must still be accepted.",
	profile = "OIDCC"
)
public class OIDCEnsureOtherScopeOrderSucceeds extends OIDCCScopeEmail {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(ReverseScopeOrderInAuthorizationEndpointRequest.class).requirement("RFC6749-3.3"));
	}

}
