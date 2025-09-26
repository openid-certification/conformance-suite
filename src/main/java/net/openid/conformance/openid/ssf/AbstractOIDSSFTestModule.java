package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.AbstractTestModule;

public abstract class AbstractOIDSSFTestModule extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		env.putString("base_url", baseUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		env.putString("ssf", "profile", getVariant(SsfProfile.class).name());

		exposeEnvString("alias", "config", "alias");

		configureServerMetadata();
		configureServerEndpoints();

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void configureServerMetadata() {

	}

	protected void configureServerEndpoints() {

	}

	@Override
	public void start() {
		// NOOP
	}

	protected boolean isSsfProfileEnabled(SsfProfile profile) {
		return profile.equals(getVariant(SsfProfile.class));
	}
}
