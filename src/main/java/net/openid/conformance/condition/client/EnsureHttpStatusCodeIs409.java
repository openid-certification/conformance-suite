package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs409 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 409;
	}

}
