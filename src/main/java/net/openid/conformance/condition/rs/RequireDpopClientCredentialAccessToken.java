package net.openid.conformance.condition.rs;


import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireDpopClientCredentialAccessToken extends AbstractConditionSequence {
	@Override
	public void evaluate() {

		// maps dpop_access_token to use the dpop_client_credentials_access_token for matching
		call(exec().startBlock("Verify DPoP Client Credentials Access Token")
			.mapKey("dpop_access_token", "dpop_client_credentials_access_token"));

		call(sequence(RequireDpopAccessToken.class));

		call(exec().unmapKey("dpop_access_token")
			.endBlock());
	}
}
