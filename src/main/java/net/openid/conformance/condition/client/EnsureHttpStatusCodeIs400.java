package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs400 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 400;
	}

}
