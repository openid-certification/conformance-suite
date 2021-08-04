package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractProtectedResourceInferenceCondition;

public class SetProtectedResourceUrlToPaymentsEndpoint extends AbstractProtectedResourceInferenceCondition {

	protected String getResourcePath() {
		return "/payments/v1/pix/payments";
	}

}
