package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

public class InjectRealCreditorAccountToPayment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("creditorAccount");

		obj.addProperty("issuer", DictHomologKeys.PROXY_EMAIL_BRANCH_NUMBER);
		obj.addProperty("number", DictHomologKeys.PROXY_EMAIL_ACCOUNT_NUMBER);
		obj.addProperty("accountType", DictHomologKeys.PROXY_EMAIL_ACCOUNT_TYPE);
		obj.addProperty("ispb", DictHomologKeys.PROXY_EMAIL_ISPB);
		logSuccess("Added real, working creditor account details to payment");

		return env;
	}
}
