package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountToPaymentConsent extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("resource", "brazilPaymentConsent.data");

		JsonObject creditor = obj.getAsJsonObject("creditor");
		creditor.addProperty("name", DictHomologKeys.PROXY_EMAIL_OWNER_NAME);
		creditor.addProperty("cpfCnpj", DictHomologKeys.PROXY_EMAIL_CPF);
		creditor.addProperty("personType", DictHomologKeys.PROXY_EMAIL_PERSON_TYPE);

		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", DictHomologKeys.PROXY_EMAIL_BRANCH_NUMBER);
		obj.addProperty("number", DictHomologKeys.PROXY_EMAIL_ACCOUNT_NUMBER);
		obj.addProperty("accountType", DictHomologKeys.PROXY_EMAIL_ACCOUNT_TYPE);
		obj.addProperty("ispb", DictHomologKeys.PROXY_EMAIL_ISPB);
		logSuccess("Added real, working creditor account details to payment consent");

		return env;
	}
}
