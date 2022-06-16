package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class EnsureJointAccountCpfOrCnpjIsPresent extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonElement cpf = env.getElementFromObject("config", "resource.brazilCpfJointAccount");
		JsonElement cnpj = env.getElementFromObject("config", "resource.brazilCnpjJointAccount");


		if (cpf == null && cnpj ==  null) {
			env.putBoolean("continue_test", false);
			throw error("Brazil CPF and CNPJ for Joint Account field is empty. Institution is assumed to not have this functionality");
		}

		if (cpf != null) {
			env.putString("config", "resource.brazilCpf", OIDFJSON.getString(cpf));
			logSuccess("Brazil CPF for Joint Account field is present and added", Map.of("CPF", cpf));
		}

		if (cnpj != null) {
			env.putString("config", "resource.brazilCnpj", OIDFJSON.getString(cnpj));
			logSuccess("Brazil CNPJ for Joint Account field is present and added", Map.of("CNPJ", cnpj));
		}
		return env;
	}
}
