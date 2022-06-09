package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddTransactionIdentificationToBrazilPixPayment extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource")
	public Environment evaluate(Environment env) {
		env.putString("resource", "brazilPixPayment.data.transactionIdentification", DictHomologKeys.PROXY_TRANSACTION_IDENTIFICATION);
		JsonElement payment = env.getElementFromObject("resource", "brazilPixPayment");
		logSuccess("Transaction identification was added to the pix payment object", Map.of("PIX payment", payment));
		return env;
	}
}
