package net.openid.conformance.condition.rs;


import net.openid.conformance.condition.as.ValidateDpopAccessToken;
import net.openid.conformance.condition.as.ValidateDpopAccessTokenfCnf;
import net.openid.conformance.condition.as.ValidateDpopProofAccessTokenHash;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireDpopAccessToken extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		call(exec().startBlock("Verify DPoP Access Token"));
		callAndStopOnFailure(ValidateDpopAccessToken.class, "DPOP-4.3");
		callAndStopOnFailure(ValidateDpopAccessTokenfCnf.class, "DPOP-4.3-12");
		callAndStopOnFailure(ValidateDpopProofAccessTokenHash.class, "DP0P-4.3.-12");
		call(exec().endBlock());
	}
}
