package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.AddInvalidNonceValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIClientType;
import net.openid.conformance.variant.VariantNotApplicable;

@VariantNotApplicable(parameter = FAPIClientType.class, values = {"plain_oauth"})
@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-nonce",
	displayName = "FAPI2-Security-Profile-Final: client test - invalid nonce in id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the nonce value in the id_token does not match the authorization request's nonce value",
	profile = "FAPI2-Security-Profile-Final",
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

public class FAPI2SPFinalClientTestInvalidNonce extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidNonceValueToIdToken.class, "OIDCC-3.1.3.7-11");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid nonce value";
	}
}
