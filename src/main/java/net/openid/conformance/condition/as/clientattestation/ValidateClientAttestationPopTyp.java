package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestationPopTyp extends AbstractValidateClientAttestationJwtTyp {

	public ValidateClientAttestationPopTyp() {
		super("client_attestation_pop_object", "oauth-client-attestation-pop+jwt", "Client attestation PoP JWT");
	}

	@Override
	@PreEnvironment(required = "client_attestation_pop_object")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
