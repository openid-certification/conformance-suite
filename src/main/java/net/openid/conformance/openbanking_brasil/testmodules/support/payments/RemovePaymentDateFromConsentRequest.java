package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemovePaymentDateFromConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject obj = (JsonObject) env.getElementFromObject("consent_endpoint_request", "data.payment");

		if(obj == null){
			logFailure("Cannot find consent_endpoint_request object.");
			return env;
		}

		obj.remove("date");

		log("Removed payment date");

		return env;
	}

}
