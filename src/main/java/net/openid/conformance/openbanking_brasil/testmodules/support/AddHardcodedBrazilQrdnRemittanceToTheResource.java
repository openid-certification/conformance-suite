package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddHardcodedBrazilQrdnRemittanceToTheResource extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		env.putString("resource", "brazilQrdnRemittance", DictHomologKeys.PROXY_EMAIL_STANDARD_REMITTANCEINFORMATION);
		logSuccess("Brazil QRDN Remittance Information was added to the Resource", Map.of("brazilQrdnRemittance", DictHomologKeys.PROXY_EMAIL_STANDARD_REMITTANCEINFORMATION));
		return env;
	}
}
