package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Removes a client attestation challenge obtained from a previously selected authorization server,
 * so a PoP JWT for the newly selected server does not carry the old server's challenge.
 */
public class RemoveClientAttestationChallenge extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		if (env.getElementFromObject("vci", "attestation_challenge") == null) {
			log("No client attestation challenge stored from a previously selected authorization server");
			return env;
		}
		env.removeElement("vci", "attestation_challenge");
		logSuccess("Removed the client attestation challenge obtained from the previously selected authorization server");
		return env;
	}
}
