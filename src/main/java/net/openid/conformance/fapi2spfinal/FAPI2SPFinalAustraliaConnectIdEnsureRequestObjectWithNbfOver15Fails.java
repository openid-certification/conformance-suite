package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-australia-connectid-ensure-request-object-with-nbf-over-15-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure request object with nbf value over 15 minutes in the past fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2AuthRequestMethod.class, values = { "unsigned" })
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "consumerdataright_au", "openbanking_brazil", "cbuae", "ksa", "openbanking_chile", "fapi_client_credentials_grant" })
public class FAPI2SPFinalAustraliaConnectIdEnsureRequestObjectWithNbfOver15Fails extends AbstractFAPI2SPFinalRequestObjectWithNbfTooFarInPastFails {

	@Override
	protected String[] getNbfRequirements() {
		return new String[] { "CID-SP-4.2-11", "FAPI2-MS-ID1-5.3.1-3" };
	}
}
