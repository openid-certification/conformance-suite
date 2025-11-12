package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIInjectCredentialConfigurationIdHint extends AbstractCondition {

	protected final String fallbackCredentialConfigurationId;

	public VCIInjectCredentialConfigurationIdHint(String fallbackCredentialConfigurationId) {
		this.fallbackCredentialConfigurationId = fallbackCredentialConfigurationId;
	}

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String credentialConfigurationIdHint = env.getString("config", "vci.credential_configuration_id");
		if (credentialConfigurationIdHint == null || credentialConfigurationIdHint.isBlank()) {
			credentialConfigurationIdHint = fallbackCredentialConfigurationId;
			log("Using implicit credential_configuration_id " + credentialConfigurationIdHint,
				args("credential_configuration_id", credentialConfigurationIdHint));
		} else {
			log("Using credential_configuration_id " + credentialConfigurationIdHint + " from test configuration",
				args("credential_configuration_id", credentialConfigurationIdHint));
		}

		boolean credentialConfigurationExists = env.getElementFromObject("credential_issuer_metadata", "credential_configurations_supported").getAsJsonObject().has(credentialConfigurationIdHint);
		if (!credentialConfigurationExists) {
			throw error("Couldn't find credential_configuration_id '" + credentialConfigurationIdHint+ "' in credential_issuer_metadata",
				args("credential_configuration_id", credentialConfigurationIdHint, "credential_issuer_metadata", env.getObject("credential_issuer_metadata")));
		}

		env.putString("credential_configuration_id_hint", credentialConfigurationIdHint);

		return env;
	}
}
