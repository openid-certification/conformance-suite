package net.openid.conformance.condition.client;

public class SetResponseTypeCodeIdTokenInDynamicRegistrationRequest extends AbstractSetResponseTypeInDynamicRegistrationRequest {

	@Override
	protected String responseType() {
		return "code id_token";
	}

}
