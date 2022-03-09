package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs200 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 200;
	}

}
