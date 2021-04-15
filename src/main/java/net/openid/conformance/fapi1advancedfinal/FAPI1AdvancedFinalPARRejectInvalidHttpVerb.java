package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsureParHTTPError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-par-attempt-invalid-http-method",
	displayName = "PAR : try to use an invalid http method ",
	summary = "This test tries to use an invalid http method and expects authorization server to return an error",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
//PAR-2.3.3 : If the request did not use POST, the authorization server shall return 405 Method Not Allowed HTTP error response.
public class FAPI1AdvancedFinalPARRejectInvalidHttpVerb extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void performParAuthorizationRequestFlow() {
		env.putString(CallPAREndpoint.HTTP_METHOD_KEY, "PUT");

		callAndStopOnFailure(CallPAREndpoint.class);

		callAndContinueOnFailure(EnsureParHTTPError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");

		fireTestFinished();
	}
}
