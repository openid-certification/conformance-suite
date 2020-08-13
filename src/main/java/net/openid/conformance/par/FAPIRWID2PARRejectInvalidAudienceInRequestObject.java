package net.openid.conformance.par;

import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi.AbstractFAPIRWID2ServerTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-2.0.1/JAR-4.0: The value of "aud" should be the value of the Authorization Server (AS) issuer
@PublishTestModule(
	testName = "fapi-rw-id2-par-pushed-authorization-url-as-audience-in-request-object",
	displayName = "PAR : try to use pushed authorization endpoint url as audience in request object",
	summary = "This test tries to use the pushed authorization endpoint url as audience in request object, the authorization server is expected to reject the request - depending when the server choices to verify the request object, the refusal may be an error from the pushed request object endpoint, or may be returned from the authorization endpoint.",
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
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
public class FAPIRWID2PARRejectInvalidAudienceInRequestObject extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void  createParAuthorizationRequestObject() {
		call(super.makeCreatePARAuthorizationRequestObjectSteps().replace(AddAudToRequestObject.class,
			condition(AddPAREndpointAsAudToRequestObject.class).requirement("PAR-2")));
	}

	protected void onAuthorizationCallbackResponse() {
		// This may be overly strict; https://tools.ietf.org/html/draft-ietf-oauth-jwsreq-26#section-4 only says 'should':
		// > The value of "aud" should be the value of the Authorization Server (AS) "issuer" as defined in RFC8414
		// It might be better to try setting 'aud' to a completely different server, which we would expect to be reject.
		callAndStopOnFailure(EnsureInvalidRequestUriError.class);
		fireTestFinished();
	}
}
