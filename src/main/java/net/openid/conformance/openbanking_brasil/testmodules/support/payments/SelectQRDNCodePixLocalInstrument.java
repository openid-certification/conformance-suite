package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractPixLocalInstrumentCondition;

public class SelectQRDNCodePixLocalInstrument extends AbstractPixLocalInstrumentCondition {
	@Override
	protected String getPixLocalInstrument() {
		return "QRDN";
	}
}
