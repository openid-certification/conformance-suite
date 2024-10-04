package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs204 extends AbstractEnsureHttpStatusCode {

	@Override
	protected int getExpectedStatusCode() {
		return 204;
	}

}
