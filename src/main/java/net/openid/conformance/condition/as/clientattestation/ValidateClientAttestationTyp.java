package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestationTyp extends AbstractValidateClientAttestationJwtTyp {

	public ValidateClientAttestationTyp() {
		super("client_attestation_object", "oauth-client-attestation+jwt", "Client attestation JWT");
	}

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
