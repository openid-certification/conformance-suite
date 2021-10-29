package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs401 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 401;
	}

}
