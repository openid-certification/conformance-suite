package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.RemoveConnectIdCiba3DSPaymentAuthorizationDetailsFromRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-authorization-details-fails",
	displayName = "ConnectID CIBA: Missing 3DS payment authorization_details should return an error",
	summary = "This test sends a ConnectID CIBA 3DS payment backchannel authentication request with no authorization_details in the signed request object. The server must return an invalid_request error.",
	profile = "FAPI-CIBA-ID1"
)
@ConfigurationFields({
	"client.card_primary_account_number"
})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithout3DSPaymentAuthorizationDetailsFails
	extends AbstractConnectIdCibaEnsureInvalid3DSPaymentAuthorizationDetailsFails {

	@Override
	protected Class<? extends Condition> getInvalidAuthorizationDetailsCondition() {
		return RemoveConnectIdCiba3DSPaymentAuthorizationDetailsFromRequestObject.class;
	}
}
