package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateAttestationChallenge extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "attestation_challenge")
	public Environment evaluate(Environment env) {

		String challenge = RandomStringUtils.secure().nextAlphanumeric(32);

		env.putString("attestation_challenge", challenge);

		logSuccess("Created attestation challenge", args("attestation_challenge", challenge));

		return env;
	}
}
