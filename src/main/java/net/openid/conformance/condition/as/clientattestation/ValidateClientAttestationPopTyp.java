package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestationPopTyp extends AbstractValidateClientAttestationJwtTyp {

	public static final String EXPECTED_TYP = "oauth-client-attestation-pop+jwt";

	public ValidateClientAttestationPopTyp() {
		super("client_attestation_pop_object", EXPECTED_TYP, "Client attestation PoP JWT");
	}

	@Override
	@PreEnvironment(required = "client_attestation_pop_object")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
