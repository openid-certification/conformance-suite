package net.openid.conformance.vciid2wallet.condition.clientattestation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateClientAttestation extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		// needs to be checked on token endpoint OR PAR endpoint

		// extract client attestation
		// extract client attestation proof

		// validate client attestation

		var valid = false;
		if (!valid) {
			// TODO add actual validation
			// throw error("Client attestation is not valid");
		}
		// if ok
		logSuccess("Validate client attestation");

		return env;
	}
}
