package net.openid.conformance.condition.client;

public class ParseCredentialAsSdJwt extends AbstractParseCredentialAsSdJwt {

	@Override
	protected boolean expectKbJwt() {
		return false;
	}
}
