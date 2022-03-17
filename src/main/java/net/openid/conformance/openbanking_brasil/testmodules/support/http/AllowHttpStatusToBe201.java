package net.openid.conformance.openbanking_brasil.testmodules.support.http;

public class AllowHttpStatusToBe201 extends AbstractOptionalHttpStatusCondition {

	@Override
	protected int getStatus() {
		return 201;
	}
}
