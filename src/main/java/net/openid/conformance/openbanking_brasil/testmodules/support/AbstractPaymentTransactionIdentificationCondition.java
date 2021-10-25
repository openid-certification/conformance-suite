package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractPaymentTransactionIdentificationCondition extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Setting transaction identification to a new value");
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPixPayment");
		obj = obj.getAsJsonObject("data");
		obj.addProperty("transactionIdentification", getTransactionIdentification());
		log(obj);
		return env;
	}

	protected abstract String getTransactionIdentification();

}
