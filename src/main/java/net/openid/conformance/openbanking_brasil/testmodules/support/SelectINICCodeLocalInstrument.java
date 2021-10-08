package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectINICCodeLocalInstrument extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "INIC";
	}

}
