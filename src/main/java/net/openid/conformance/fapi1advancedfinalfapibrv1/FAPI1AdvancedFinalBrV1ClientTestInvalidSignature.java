package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.condition.as.InvalidateIdTokenSignature;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-br-v1-client-test-invalid-signature",
	displayName = "FAPI1-Advanced-Final-Br-v1: client test - invalid signature in id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the signature in the id_token from the authorization_endpoint does not match the signature value in the request object",
	profile = "FAPI1-Advanced-Final-Br-v1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = {"jarm"})
public class FAPI1AdvancedFinalBrV1ClientTestInvalidSignature extends AbstractFAPI1AdvancedFinalBrV1ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(InvalidateIdTokenSignature.class, "OIDCC-3.1.3.7-6");

	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid signature";
	}
}