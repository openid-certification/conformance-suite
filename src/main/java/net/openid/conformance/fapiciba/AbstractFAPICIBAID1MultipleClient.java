package net.openid.conformance.fapiciba;

public abstract class AbstractFAPICIBAID1MultipleClient extends AbstractFAPICIBAID1 {

	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("client_public_jwks", "client_public_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("client_public_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

	/** Return which client is in use, for use in block identifiers */
	@Override
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		}
		return "";
	}

	@Override
	protected void configClient() {
		setupClient1();

		setupClient2();
	}

	@Override
	public void cleanup() {
		unregisterClient1();

		unregisterClient2();
	}
}
