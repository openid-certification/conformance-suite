package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddExtraFoobarToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_NotUnderstood
@PublishTestModule(
	testName = "oidcc-ensure-request-with-unknown-parameter-succeeds",
	displayName = "OIDCC: ensure request with unknown parameter succeeds.",
	summary = "The test includes the parameter extra=foobar (which is not defined by any specification) in the request to the authorization endpoint, and the authentication must complete successfully with the extra parameter ignored as per RFC6749-3.1 'The authorization server MUST ignore unrecognized request parameters'",
	profile = "OIDCC"
)
public class OIDCCEnsureRequestWithUnknownParameterSucceeds extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(condition(AddExtraFoobarToAuthorizationEndpointRequest.class).requirements("RFC6749-3.1"));
	}

}
