package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPaymentLocalInstrumentCondtion extends AbstractCondition {

	@Override
	public final Environment evaluate(Environment env) {
		log("Setting local instrument to a new value");
		JsonObject obj = env.getObject("resource");

		final String errorMessage = "Configuration is malformed. Missing an expected JSON field - ";
		StringBuilder missingMember = new StringBuilder("brazilQrdnPaymentConsent");

		if (obj.has("brazilQrdnPaymentConsent")) {
			obj = getPaymentConsentObject(obj);
			List<String> members = new LinkedList<>(Arrays.asList("data", "payment", "details"));
			for (String member : members) {
				missingMember.append(" > ").append(member);
				if (obj.has(member)) {
					obj = obj.getAsJsonObject(member);
				} else {
					throw error(errorMessage + missingMember);
				}
			}
			obj.addProperty("localInstrument", getLocalInstrument());
			log(obj);
			return env;
		}
		throw error(errorMessage + missingMember);
	}

	protected JsonObject getPaymentConsentObject(JsonObject resourceConfig) {
		return resourceConfig.getAsJsonObject("brazilPaymentConsent");
	}

	protected abstract String getLocalInstrument();

}
