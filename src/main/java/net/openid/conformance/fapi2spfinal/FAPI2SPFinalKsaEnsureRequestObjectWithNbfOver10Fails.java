package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ksa-ensure-request-object-with-nbf-over-10-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure request object with nbf value over 10 minutes in the past fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2AuthRequestMethod.class, values = { "unsigned" })
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "consumerdataright_au", "openbanking_brazil", "connectid_au", "cbuae", "openbanking_chile", "fapi_client_credentials_grant" })
public class FAPI2SPFinalKsaEnsureRequestObjectWithNbfOver10Fails extends AbstractFAPI2SPFinalRequestObjectWithNbfTooFarInPastFails {

	@Override
	protected String[] getNbfRequirements() {
		return new String[] { "KSA-OF-1", "RFC7519-4.1.5" };
	}
}
