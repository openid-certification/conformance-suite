package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountPhoneToPaymentConsent extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data");

		JsonObject creditor = obj.getAsJsonObject("creditor");
		creditor.addProperty("name", DictHomologKeys.PROXY_PHONE_NUMBER_OWNER_NAME);
		creditor.addProperty("cpfCnpj", DictHomologKeys.PROXY_PHONE_NUMBER_CPF);
		creditor.addProperty("personType", DictHomologKeys.PROXY_PHONE_NUMBER_PERSON_TYPE);

		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", DictHomologKeys.PROXY_PHONE_NUMBER_BRANCH_NUMBER);
		obj.addProperty("number", DictHomologKeys.PROXY_PHONE_NUMBER_ACCOUNT_NUMBER);
		obj.addProperty("accountType", DictHomologKeys.PROXY_PHONE_NUMBER_ACCOUNT_TYPE);
		obj.addProperty("ispb", DictHomologKeys.PROXY_PHONE_NUMBER_ISPB);
		logSuccess("Added real, working creditor account details to payment consent");

		return env;
	}
}
