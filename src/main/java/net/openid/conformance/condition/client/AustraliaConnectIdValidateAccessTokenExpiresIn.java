package net.openid.conformance.condition.client;

public class AustraliaConnectIdValidateAccessTokenExpiresIn extends AbstractValidateAccessTokenExpiresInMaximum {

	@Override
	protected int getMaximumSeconds() {
		return 600;
	}

}
