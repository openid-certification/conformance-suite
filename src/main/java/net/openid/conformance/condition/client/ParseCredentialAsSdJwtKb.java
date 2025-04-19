package net.openid.conformance.condition.client;

public class ParseCredentialAsSdJwtKb extends AbstractParseCredentialAsSdJwt {

	@Override
	protected boolean expectKbJwt() {
		return true;
	}
}
