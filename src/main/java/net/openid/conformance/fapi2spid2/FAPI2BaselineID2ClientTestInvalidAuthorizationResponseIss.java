package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.AddInvalidIssToAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {
	"jarm"
})
@PublishTestModule(
	testName = "fapi2-securityprofile-id2-client-test-invalid-authorization-response-iss",
	displayName = "FAPI2-SecurityProfile-ID2: client test - invalid aud in id_token from token_endpoint must be rejected",
	summary = "This test sends an invalid issuer in the token_endpoint response ID Token. The client should display an invalid issuer message and must not call any other endpoints.",
	profile = "FAPI2-SecurityProfile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestInvalidAuthorizationResponseIss extends AbstractFAPI2BaselineID2ClientExpectNothingAfterAuthorizationResponse {
	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Returned invalid iss";
	}

	@Override
	protected void addCustomValuesToAuthorizationResponse() {
		callAndContinueOnFailure(AddInvalidIssToAuthorizationEndpointResponseParams.class);
	}

	@Override
	protected void addCustomValuesToIdToken() {
		// do nothing
	}
}
