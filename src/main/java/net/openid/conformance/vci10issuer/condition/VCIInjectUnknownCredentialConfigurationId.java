package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class VCIInjectUnknownCredentialConfigurationId extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String actualCredentialConfigurationId = env.getString("vci_credential_configuration_id");
		String newCredentialConfigurationId = UUID.randomUUID().toString();
		env.putString("vci_credential_configuration_id", newCredentialConfigurationId);

		log("Override actual credential_configuration_id with random value",
			args("actual_credential_configuration_id", actualCredentialConfigurationId, "new_credential_configuration_id", newCredentialConfigurationId));

		return env;
	}
}
