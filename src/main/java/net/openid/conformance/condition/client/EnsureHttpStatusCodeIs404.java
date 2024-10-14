package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs404 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 404;
	}

}
