package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractProtectedResourceInferenceCondition;

public class SetProtectedResourceUrlToPaymentsEndpoint extends AbstractProtectedResourceInferenceCondition {

	@Override
	protected String getResourcePath() {
		return "/open-banking/payments/v1/pix/payments";
	}

}
