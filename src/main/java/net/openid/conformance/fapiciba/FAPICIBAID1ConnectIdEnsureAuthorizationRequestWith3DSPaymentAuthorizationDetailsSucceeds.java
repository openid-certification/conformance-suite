package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintToCardPrimaryAccountNumber;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-3ds-payment-authorization-details-succeeds",
	displayName = "ConnectID CIBA: Test with 3DS payment authorization_details, the server must authenticate successfully",
	summary = "This test sends a ConnectID CIBA backchannel authentication request for the 3DS payment authentication use case. The signed request object includes binding_message, a card primary account number login_hint, and valid authorization_details of type 3ds:payment_authorisation. The server must authenticate successfully.",
	profile = "FAPI-CIBA-ID1"
)
@ConfigurationFields({
	"client.card_primary_account_number"
})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWith3DSPaymentAuthorizationDetailsSucceeds extends AbstractFAPICIBAID1 {

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();
		callAndStopOnFailure(SetConnectIdCibaLoginHintToCardPrimaryAccountNumber.class,
			"CID-CIBA-4.1.3.1", "CID-CIBA-4.3-1");
	}

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject.class,
			"CID-CIBA-4.1.3.1", "RAR-2");
	}
}
