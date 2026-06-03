package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class RemoveConnectIdCiba3DSPaymentInstructedAmount
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.remove("instructed_amount");
	}
}
