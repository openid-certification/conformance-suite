package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddWrongIssToClientAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.EnsurePARInvalidClientOrInvalidRequestError;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-par-ensure-client-assertion-with-wrong-iss-fails",
	displayName = "FAPI1-Advanced-Final: PAR: ensure client_assertion with wrong iss fails",
	summary = "This test sends a client assertion with a wrong 'iss' value (not equal to the client_id) to the PAR endpoint. Per RFC 9126 section 2, authentication at the PAR endpoint must follow the same rules as the token endpoint. Per RFC7523 section 3 and OIDC Core section 9 the iss claim must contain the client_id. The server must reject this request.",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = { "by_value" })
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPI1AdvancedFinalPAREnsureClientAssertionWithWrongIssFails extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		mapClientAuthKeys("pushed_authorization_request_form_parameters",
			"pushed_authorization_request_endpoint_request_headers");
		call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest()
			.insertAfter(CreateClientAuthenticationAssertionClaims.class,
				condition(AddWrongIssToClientAssertionClaims.class).requirements("RFC7523-3", "OIDCC-9")));
		unmapClientAuthKeys();
	}

	@Override
	protected void processParResponse() {
		callAndContinueOnFailure(CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-5.2");
		callAndContinueOnFailure(EnsurePARInvalidClientOrInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");

		fireTestFinished();
	}
}
