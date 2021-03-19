package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.client.ReverseScopeOrderInAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-other-scope-order-succeeds",
	displayName = "FAPI-RW-ID2: ensure other scope order succeeds",
	summary = "This test makes a FAPI authorization request but with the order of the entries in the 'scope' reversed, which must succeed. As per RFC6749 section 3.3, 'If the value contains multiple space-delimited strings, their order does not matter'. The reason for this test is that some OAuth clients process scopes in a way that the order they are sent to the server is not under the control of a developer using that client, and as per the spec such requests must still be accepted.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPIRWID2EnsureOtherScopeOrderSucceeds extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(ReverseScopeOrderInAuthorizationEndpointRequest.class).requirement("RFC6749-3.3"));
	}
}
