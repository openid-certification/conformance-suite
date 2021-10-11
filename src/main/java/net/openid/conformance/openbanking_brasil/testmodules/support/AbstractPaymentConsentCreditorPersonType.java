package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractPaymentConsentCreditorPersonType extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Setting payment person type to a new value");
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("creditor");
		obj.addProperty("personType", getConsentPersonType());
		log(obj);
		return env;
	}

	protected abstract String getConsentPersonType();

}
