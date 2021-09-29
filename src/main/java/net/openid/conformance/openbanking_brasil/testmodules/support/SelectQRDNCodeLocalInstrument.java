package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectQRDNCodeLocalInstrument extends AbstractPaymentLocalInstrumentCondtion {

	@Override
	protected String getLocalInstrument() {
		return "QRDN";
	}

}
