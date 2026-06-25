package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class RemoveConnectIdCiba3DSPaymentBeneficiaryName
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.remove("beneficiary_name");
	}
}
