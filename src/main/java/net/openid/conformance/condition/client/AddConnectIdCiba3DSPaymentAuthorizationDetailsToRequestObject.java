package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject extends AbstractCondition {

	public static final String TYPE = "3ds:payment_authorisation";

	@Override
	@PreEnvironment(required = { "config", "request_object_claims" })
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		String cardPrimaryAccountNumber = env.getString("config", "client.card_primary_account_number");
		if (Strings.isNullOrEmpty(cardPrimaryAccountNumber)) {
			throw error("'Card primary account number' field is missing from the 'Client' section in the test configuration");
		}
		double amount = getRequiredPaymentAmount(env);
		String currency = getRequiredPaymentString(env, "client.payment_currency", "Payment currency");
		String beneficiaryName = getRequiredPaymentString(env, "client.payment_beneficiary_name", "Payment beneficiary name");
		String paymentDesc = getRequiredPaymentString(env, "client.payment_desc", "Payment description");

		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.add("authorization_details",
			createAuthorizationDetails(cardPrimaryAccountNumber, amount, currency, beneficiaryName, paymentDesc));

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added ConnectID 3DS payment authorization_details to request object claims",
			args("authorization_details", requestObjectClaims.get("authorization_details"),
				"request_object_claims", requestObjectClaims));

		return env;
	}

	protected JsonArray createAuthorizationDetails(String cardPrimaryAccountNumber, double amount,
												   String currency, String beneficiaryName, String paymentDesc) {
		JsonObject instructedAmount = new JsonObject();
		instructedAmount.addProperty("amount", amount);
		instructedAmount.addProperty("currency", currency);

		JsonObject authorizationDetails = new JsonObject();
		authorizationDetails.addProperty("type", TYPE);
		authorizationDetails.add("instructed_amount", instructedAmount);
		authorizationDetails.addProperty("source_account", cardPrimaryAccountNumber);
		authorizationDetails.addProperty("beneficiary_name", beneficiaryName);
		authorizationDetails.addProperty("payment_desc", paymentDesc);

		JsonArray authorizationDetailsArray = new JsonArray();
		authorizationDetailsArray.add(authorizationDetails);
		return authorizationDetailsArray;
	}

	private double getRequiredPaymentAmount(Environment env) {
		String amount = getRequiredPaymentString(env, "client.payment_amount", "Payment amount");
		try {
			return Double.parseDouble(amount);
		} catch (NumberFormatException e) {
			throw error("'Payment amount' field in the 'Client' section in the test configuration must be a number",
				args("payment_amount", amount));
		}
	}

	private String getRequiredPaymentString(Environment env, String path, String label) {
		String value = env.getString("config", path);
		if (Strings.isNullOrEmpty(value)) {
			throw error("'%s' field is missing from the 'Client' section in the test configuration".formatted(label));
		}
		return value;
	}
}
