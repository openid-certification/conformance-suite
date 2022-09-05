package net.openid.conformance.condition.client;

public class SetResponseTypeCodeInDynamicRegistrationRequest extends AbstractSetResponseTypeInDynamicRegistrationRequest {

	@Override
	protected String responseType() {
		return "code";
	}

}
