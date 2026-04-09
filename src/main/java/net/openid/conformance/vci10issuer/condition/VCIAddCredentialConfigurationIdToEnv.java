package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.TestFailureException;

public class VCIAddCredentialConfigurationIdToEnv extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw new TestFailureException(getTestId(), "credential_configuration_id cannot be null or empty!");
		}

		env.putString("vci_credential_configuration_id", vciCredentialConfigurationId);
		return env;
	}
}
