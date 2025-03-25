package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.AddInvalidOpenBankingIntentIdToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-openbanking-intent-id",
	displayName = "FAPI2-Security-Profile-Final: client test  - invalid openbanking_intent_id, should be rejected",
	summary = "This test should end with the client displaying an error message that the openbanking_intent_id returned in id_token from authorization endpoint does not match the value sent in the request object",
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
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae" })
public class FAPI2SPFinalClientTestInvalidOpenBankingIntentId extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidOpenBankingIntentIdToIdToken.class, "OBSP-3.3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid openbanking_intent_id value";
	}
}
