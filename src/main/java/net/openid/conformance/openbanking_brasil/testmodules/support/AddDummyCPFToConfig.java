package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDummyCPFToConfig extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config" )
	public Environment evaluate(Environment env) {

		String dummyCPF = "11111111111";
		env.putString("config", "resource.brazilCpf", dummyCPF);
		return env;
	}
}
