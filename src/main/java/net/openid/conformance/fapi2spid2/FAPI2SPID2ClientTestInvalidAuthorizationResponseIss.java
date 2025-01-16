package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidIssToAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {
	"jarm"
})
@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-invalid-authorization-response-iss",
	displayName = "FAPI2-Security-Profile-ID2: client test - invalid iss from authorization response must be rejected",
	summary = "This test sends an invalid iss in the authorization response. The client should display an invalid issuer message and must not call any other endpoints.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2ClientTestInvalidAuthorizationResponseIss extends AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse {
	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Returned invalid iss";
	}

	@Override
	protected void addCustomValuesToAuthorizationResponse() {
		callAndContinueOnFailure(AddInvalidIssToAuthorizationEndpointResponseParams.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void addCustomValuesToIdToken() {
		// do nothing
	}
}
