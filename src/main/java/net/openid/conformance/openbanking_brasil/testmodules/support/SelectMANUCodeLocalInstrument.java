package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectMANUCodeLocalInstrument extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "MANU";
	}

}
