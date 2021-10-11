package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectBADPaymentType extends AbstractPaymentConsentPaymentTypeCondition {
	@Override
	protected String getPaymentType() {
		return "BAD";
	}
}
