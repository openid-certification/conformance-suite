package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddExpValueOf179DaysToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-validity-exp-test",
	displayName = "FAPI-CIBA-ID1: Client test - exp value in id_token is less than 180 days; should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token " +
		"from the token endpoint does not have a validity of at least 180 days",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"plain_fapi", "consumerdataright_au", "openbanking_uk", "openinsurance_brazil", "openbanking_ksa"})
public class FAPICIBAClientInvalidValidityExpTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddExpValueOf179DaysToIdToken.class, "BrazilCIBA-5.2.2");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "exp not valid for more than 180 days";
	}

}
