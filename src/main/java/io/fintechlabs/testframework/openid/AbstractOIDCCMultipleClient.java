package io.fintechlabs.testframework.openid;

import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;

public abstract class AbstractOIDCCMultipleClient extends AbstractOIDCCServerTest {

	@Override
	protected void configureClient() {
		super.configureClient();

		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		validateClientConfiguration();
		unmapClient();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			switchToSecondClient();
			performAuthorizationFlow();
		} else {
			performSecondClientTests();
			super.onPostAuthorizationFlowComplete();
		}
	}

	/**
	 * These are additional tests involving the second client,
	 * performed after the usual post-authorization flow.
	 */
	protected abstract void performSecondClientTests();

	@Override
	protected String currentClientString() {
		if (isSecondClient()) {
			return "Second client: ";
		} else {
			return "";
		}
	}

	protected boolean isSecondClient() {
		return env.isKeyMapped("client");
	}

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
}
