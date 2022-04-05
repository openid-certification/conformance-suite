package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.AddInvalidOpenBankingIntentIdToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-openbanking-intent-id",
	displayName = "FAPI2-Baseline-ID2: client test  - invalid openbanking_intent_id, should be rejected",
	summary = "This test should end with the client displaying an error message that the openbanking_intent_id returned in id_token from authorization endpoint does not match the value sent in the request object",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)
@VariantNotApplicable(parameter = FAPI2ID2OPProfile.class, values = { "plain_fapi", "openbanking_brazil", "idmvp" })
public class FAPI2BaselineID2ClientTestInvalidOpenBankingIntentId extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddInvalidOpenBankingIntentIdToIdToken.class, "OBSP-3.3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid openbanking_intent_id value";
	}
}
