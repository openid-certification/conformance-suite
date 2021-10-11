package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractPaymentConsentPaymentTypeCondition extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Setting payment type to a new value");
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj.addProperty("type", getPaymentType());
		log(obj);
		return env;
	}

	protected abstract String getPaymentType();

}
