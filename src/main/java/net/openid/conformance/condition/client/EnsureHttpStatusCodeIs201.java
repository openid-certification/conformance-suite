package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs201 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 201;
	}

}
