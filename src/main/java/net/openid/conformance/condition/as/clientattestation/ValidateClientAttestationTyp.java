package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestationTyp extends AbstractValidateClientAttestationJwtTyp {

	public static final String EXPECTED_TYP = "oauth-client-attestation+jwt";

	public ValidateClientAttestationTyp() {
		super("client_attestation_object", EXPECTED_TYP, "Client attestation JWT");
	}

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
