package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectDICTCodeLocalInstrument extends AbstractPaymentLocalInstrumentCondtion {
	@Override
	protected String getLocalInstrument() {
		return "DICT";
	}
}
