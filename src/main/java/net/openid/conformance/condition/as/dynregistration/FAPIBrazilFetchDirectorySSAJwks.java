package net.openid.conformance.condition.as.dynregistration;

public class FAPIBrazilFetchDirectorySSAJwks extends AbstractFAPIBrazilFetchDirectorySSAJwks {

	@Override
	protected String getJwksPath() {
		return "openbanking.jwks";
	}
}
