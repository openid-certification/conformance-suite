package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

public class SetConnectIdCiba3DSPaymentSourceAccountToInvalid
	extends AbstractSetInvalidConnectIdCiba3DSPaymentAuthorizationDetails {

	public static final String INVALID_SOURCE_ACCOUNT = "not-a-card-primary-account-number";

	@Override
	protected void modifyAuthorizationDetails(JsonObject requestObjectClaims, JsonObject authorizationDetails) {
		authorizationDetails.addProperty("source_account", INVALID_SOURCE_ACCOUNT);
	}
}
