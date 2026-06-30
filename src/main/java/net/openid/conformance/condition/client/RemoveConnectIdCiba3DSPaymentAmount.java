package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class RemoveConnectIdCiba3DSPaymentAmount
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.getAsJsonObject("instructed_amount").remove("amount");
	}
}
