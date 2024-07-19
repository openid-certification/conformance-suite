package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.RemoveIssFromAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {
	"jarm"
})
@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-remove-authorization-response-iss",
	displayName = "FAPI2-Security-Profile-ID2: client test - authorization_endpoint response without iss must be rejected",
	summary = "This test does not send an issuer in the authorization response. The client should display a message that the authorization response does not contain an issuer and must not call any other endpoints.",
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

public class FAPI2SPID2ClientTestRemoveAuthorizationResponseIss extends AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse {
	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Removed iss from authorization response";
	}

	@Override
	protected void addCustomValuesToAuthorizationResponse() {
		callAndContinueOnFailure(RemoveIssFromAuthorizationEndpointResponseParams.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void addCustomValuesToIdToken() {
		// do nothing
	}
}
