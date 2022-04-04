package net.openid.conformance.condition.rs;


import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireDpopRefreshToken extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		call(exec().startBlock("Verify DPoP Refresh Token")
			.mapKey("incoming_dpop_access_token", "incoming_dpop_refresh_token"));

		call(sequence(RequireDpopAccessToken.class));

		call(exec().unmapKey("incoming_dpop_access_token")
			.endBlock());
	}
}
