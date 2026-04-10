package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIAddCredentialConfigurationIdToEnv extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "vci_credential_configuration_id")
	public Environment evaluate(Environment env) {

		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw error("'Credential Configuration ID' is missing or empty in the test configuration");
		}

		env.putString("vci_credential_configuration_id", vciCredentialConfigurationId);

		log("Extracted credential_configuration_id from config",
			args("vci_credential_configuration_id", vciCredentialConfigurationId));
		return env;
	}
}
