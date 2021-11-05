package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountToPaymentPhone extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", DictHomologKeys.PROXY_PHONE_NUMBER_BRANCH_NUMBER);
		obj.addProperty("number", DictHomologKeys.PROXY_PHONE_NUMBER_ACCOUNT_NUMBER);
		obj.addProperty("accountType", DictHomologKeys.PROXY_PHONE_NUMBER_ACCOUNT_TYPE);
		obj.addProperty("ispb", DictHomologKeys.PROXY_PHONE_NUMBER_ISPB);
		logSuccess("Added real, working creditor account details to payment");

		return env;
	}
}
