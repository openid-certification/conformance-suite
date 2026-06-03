package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.RemoveConnectIdCiba3DSPaymentBeneficiaryName;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-without-3ds-payment-beneficiary-name-fails",
	displayName = "ConnectID CIBA: Missing 3DS payment beneficiary_name should return an error",
	summary = "This test sends a ConnectID CIBA 3DS payment backchannel authentication request with authorization_details missing beneficiary_name. The server must return an invalid_request error.",
	profile = "FAPI-CIBA-ID1"
)
@ConfigurationFields({
	"client.card_primary_account_number"
})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithout3DSPaymentBeneficiaryNameFails
	extends AbstractConnectIdCibaEnsureInvalid3DSPaymentAuthorizationDetailsFails {

	@Override
	protected Class<? extends Condition> getInvalidAuthorizationDetailsCondition() {
		return RemoveConnectIdCiba3DSPaymentBeneficiaryName.class;
	}
}
