package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectInvalidPersonType extends AbstractPaymentConsentCreditorPersonType {
	@Override
	protected String getConsentPersonType() { return "INVALID"; }
}
