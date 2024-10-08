package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.AbstractTestModule;

public abstract class AbstractOIDSSFTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	@Override
	public void start() {

	}
}
