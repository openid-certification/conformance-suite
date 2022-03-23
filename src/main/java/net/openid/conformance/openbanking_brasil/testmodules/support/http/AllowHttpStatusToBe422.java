package net.openid.conformance.openbanking_brasil.testmodules.support.http;

public class AllowHttpStatusToBe422 extends AbstractOptionalHttpStatusCondition {

	@Override
	protected int getStatus() {
		return 422;
	}
}
