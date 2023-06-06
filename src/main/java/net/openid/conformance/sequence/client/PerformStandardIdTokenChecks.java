package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForSubjectInIdToken;
import net.openid.conformance.condition.client.EnsureIdTokenUpdatedAtValid;
import net.openid.conformance.condition.client.ValidateEncryptedIdTokenHasKid;
import net.openid.conformance.condition.client.ValidateIdToken;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import net.openid.conformance.condition.client.ValidateIdTokenNonce;
import net.openid.conformance.condition.client.ValidateIdTokenSignature;
import net.openid.conformance.condition.client.ValidateIdTokenStandardClaims;
import net.openid.conformance.sequence.AbstractConditionSequence;

// This class is intended to perform all checks that will always be true for an id_token
// It should only contain checks that are obviously correct in all circumstances
// e.g. it cannot require that auth_time is very recent (as the user may have logged in some time ago)
// It can check optional items, so long as it doesn't insist optional items are present (unless they are
// required in a circumstance it can detect based on the environment, e.g. nonce required if the request
// contained a nonce).
public class PerformStandardIdTokenChecks extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateIdToken.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7");
		callAndContinueOnFailure(ValidateIdTokenStandardClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");

		// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#verify_nonce
		// and https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#check_idtoken_nonce
		callAndContinueOnFailure(ValidateIdTokenNonce.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckForSubjectInIdToken.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		callAndContinueOnFailure(EnsureIdTokenUpdatedAtValid.class, Condition.ConditionResult.FAILURE, "OIDCC-5.1");

		call(condition(ValidateEncryptedIdTokenHasKid.class)
			.skipIfElementMissing("id_token", "jwe_header")
			.onFail(Condition.ConditionResult.FAILURE)
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("OIDCC-10.2", "OIDC-10.2.1")
			.dontStopOnFailure()
		);
	}

}
