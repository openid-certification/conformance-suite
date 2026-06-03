package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class SetConnectIdCiba3DSPaymentAuthorizationDetailsTypeToInvalid
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.addProperty("type", "payment");
	}
}
