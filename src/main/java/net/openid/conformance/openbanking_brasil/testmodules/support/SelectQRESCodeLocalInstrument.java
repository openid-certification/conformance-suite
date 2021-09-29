package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectQRESCodeLocalInstrument extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "QRES";
	}

}
