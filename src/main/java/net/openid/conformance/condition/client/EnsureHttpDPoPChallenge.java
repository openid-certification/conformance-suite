package net.openid.conformance.condition.client;

public class EnsureHttpDPoPChallenge extends AbstractEnsureHttpAuthorizeChallenge {

	protected String getExpectedChallenge(){
		return "DPoP";
	}
}
