package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBadRedirectUriToRequestParameters;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-2.0.1:  HTTP verb POST
//PAR-2.0.2:  Content-Type: application/x-www-form-urlencoded
//PAR-2.1.2 : The AS MUST validate the request in the same way as at the authorization endpoint.
// For example, the authorization server checks whether the redirect URI matches one of the redirect URIs
// configured for the client. It MUST also check whether the client is authorized for the scope for which
// it is requesting access. This validation allows the authorization server to refuse unauthorized or
// fraudulent requests early.
//PAR-2.3.2 : The requested redirect URI is invalid or mismatching. The AS responds with HTTP status code 400 and error value invalid_redirect_uri.

//PAR-3.1.1 : If the signature validation fails, the authorization server shall return 401 Unauthorized HTTP error response. The same applies if the client_id or, if applicable, the iss claim in the request object do not match the authenticated client_id.

@PublishTestModule(
	testName = "fapi-rw-id2-par-attempt-invalid-redirect_uri",
	displayName = "PAR : try to use an invalid redirect_uri ",
	summary = "This test tries to provide an invalid redirect_uri and expects authorization server to return an error",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
public class FAPIRWID2PARRejectInvalidRedirectUri extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "FAPI-R-5.2.2-9", "PAR-2.3");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_missing_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps().
			butFirst(condition(AddBadRedirectUriToRequestParameters.class));
	}

	@Override
	protected void performParAuthorizationRequestFlow() {
		callAndStopOnFailure(CallPAREndpoint.class);

		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");

		fireTestFinished();
	}
}
