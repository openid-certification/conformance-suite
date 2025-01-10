package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs403 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 404;
	}

}
