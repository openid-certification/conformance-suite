package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-state-only-outside-request-object-not-used",
	displayName = "FAPI1-Advanced-Final: check that a state value passed only outside request object is not used",
	summary = "This test uses a request object that does not contain state, but state is passed in the url parameters to the authorization endpoint (hence state should be ignored, as FAPI-RW says only parameters inside the request object should be used). The expected result is a successful authentication that returns neither state nor s_hash. It is also permissible to show (a screenshot of which should be uploaded) or return an error message: invalid_request, invalid_request_object or access_denied.",
	profile = "FAPI1-Advanced-Final",
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
public class FAPI1AdvancedFinalEnsureRequestObjectWithoutState extends AbstractFAPI1AdvancedFinalEnsureRequestObjectWithoutState {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
			.insertAfter(SignRequestObject.class,
				condition(AddStateToAuthorizationEndpointRequest.class));
	}

}
