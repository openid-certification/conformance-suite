package net.openid.conformance.condition.client;

public class EnsureResourceResponseReturnedJwtContentType extends AbstractEnsureResourceResponseReturnedContentType {

	@Override
	protected String expectedSubtype() {
		return "jwt";
	}

}
