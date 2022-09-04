package net.openid.conformance.condition.client;

public class EnsureResourceResponseReturnedJsonContentType extends AbstractEnsureResourceResponseReturnedContentType {

	@Override
	protected String expectedSubtype() {
		return "json";
	}

}
