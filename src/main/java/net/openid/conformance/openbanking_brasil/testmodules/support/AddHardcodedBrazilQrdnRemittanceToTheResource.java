package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.DictHomologKeys;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddHardcodedBrazilQrdnRemittanceToTheResource extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		String proxyEmailStandardRemittanceInformation = DictHomologKeys.PROXY_QRDN_EDIT_PAYMENT_REMITTANCE_INFORMATION;
		env.putString("resource", "brazilQrdnRemittance", proxyEmailStandardRemittanceInformation);
		logSuccess("Brazil QRDN Remittance Information was added to the Resource", Map.of("brazilQrdnRemittance", proxyEmailStandardRemittanceInformation));
		return env;
	}
}
