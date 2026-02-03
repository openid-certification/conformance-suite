package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class VCIInjectUnknownCredentialIdentifier extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialIdentifier = "unknown:" + UUID.randomUUID();
		env.putString("vci_credential_identifier", credentialIdentifier);

		log("Inject credential_identifier with random value",
			args("new_credential_identifier", credentialIdentifier) );

		return env;
	}
}
