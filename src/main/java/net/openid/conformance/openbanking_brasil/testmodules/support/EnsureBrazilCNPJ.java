package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBrazilCNPJ extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate (Environment env) {
		String brazilCNPJ = env.getString("config", "resource.brazilCnpj");

		if(Strings.isNullOrEmpty(brazilCNPJ)) {
			throw error("brazilCNPJ is missing.");
		}

		logSuccess("brazilCNPJ was successfully found.", args("brazilCnpj", brazilCNPJ));
		return env;
	}
}
