package net.openid.conformance.openbanking_brasil.testmodules.support;

public class SelectINICCodePixLocalInstrument extends AbstractPixLocalInstrumentCondition {
	@Override
	protected String getPixLocalInstrument() {
		return "INIC";
	}
}
