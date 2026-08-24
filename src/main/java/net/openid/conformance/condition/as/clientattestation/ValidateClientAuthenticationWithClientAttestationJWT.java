package net.openid.conformance.condition.as.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateClientAuthenticationWithClientAttestationJWT extends AbstractConditionSequence {

	private static final String[] CLIENT_ATTESTATION_OBJECTS = { "client_attestation_object", "client_attestation_pop_object" };

	@Override
	public void evaluate() {

		// none of these are fatal to the test: a client that sends a missing or bad attestation should still
		// get all of the remaining problems reported in one run, rather than one per run. The conditions below
		// skip themselves when there is no attestation at all to check.
		callAndContinueOnFailure(ExtractClientAttestationFromRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		// Validate the client attestation JWT signature using x5c public key
		call(condition(ValidateClientAttestationSignature.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-6.2")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationCnfJwkFields.class)
			.onFail(Condition.ConditionResult.WARNING)
			.requirements("OAuth2-ATCA07-5.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		// Validate the client attestation pop signature using cnf.jwk from attestation
		call(condition(ValidateClientAttestationKeyBindingSignature.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-6.2")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(CheckForClientAttestationProofJwtReuse.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-10.2", "OAuth2-ATCA07-5.2")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationIssuer.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-6.2")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationSubject.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationExpiration.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.1", "OAuth2-ATCA07-6.2")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationIssuedAt.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationNotBefore.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationProofJwtAudience.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.2-5.2.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		call(condition(ValidateClientAttestationX5cClaimInProofJwt.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.2-5.2.1")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
		// Validates challenge if attestation_challenge is present in the environment (i.e., challenge endpoint was used)
		call(condition(ValidateClientAttestationProofJwtChallenge.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("OAuth2-ATCA07-5.2", "OAuth2-ATCA07-8")
			.skipIfObjectsMissing(CLIENT_ATTESTATION_OBJECTS)
			.dontStopOnFailure());
	}
}
