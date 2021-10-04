package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.client.AbstractEnsureResourceResponseReturnedContentType;

public class EnsureResourceResponseReturnedJwtContentType extends AbstractEnsureResourceResponseReturnedContentType {
	@Override
	protected String expectedSubtype() {
		return "jwt";
	}
}
