package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class InjectCorrectButUnknownCpf extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		var cpf = "51382920725";
		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");
		obj = obj.getAsJsonObject("details");
		obj.addProperty("proxy", cpf);

		logSuccess("Added correct but unregistered CPF to payment consent", Map.of("CPF", cpf));

		return env;
	}
}
