package net.openid.conformance.condition.as.dynregistration;

public class FAPIBrazilOpInFetchDirectorySSAJwks extends AbstractFAPIBrazilFetchDirectorySSAJwks {

	@Override
	protected String getJwksPath() {
		return "openinsurance.jwks";
	}
}
