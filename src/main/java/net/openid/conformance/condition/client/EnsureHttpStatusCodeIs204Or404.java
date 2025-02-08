package net.openid.conformance.condition.client;

public class EnsureHttpStatusCodeIs204Or404 extends EnsureHttpStatusCodeIsAnyOf {

	public EnsureHttpStatusCodeIs204Or404() {
		super(204, 404);
	}
}
