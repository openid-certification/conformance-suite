package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class SetConnectIdCiba3DSPaymentCurrencyToInvalid
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.getAsJsonObject("instructed_amount").addProperty("currency", "NOT_A_CURRENCY");
	}
}
