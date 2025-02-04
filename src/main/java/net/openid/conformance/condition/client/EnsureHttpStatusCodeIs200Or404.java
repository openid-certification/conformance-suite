package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs200Or404 extends EnsureHttpStatusCodeIsAnyOf {

	public EnsureHttpStatusCodeIs200Or404() {
		super(200, 404);
	}
}
