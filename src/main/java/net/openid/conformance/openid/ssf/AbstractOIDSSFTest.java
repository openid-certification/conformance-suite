package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscEndpointIssuer;
import net.openid.conformance.openid.ssf.conditions.CheckTransmitterMetadataIssuer;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGetDynamicTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGetStaticTransmitterConfiguration;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ServerMetadata;

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

	protected void fetchTransmitterMetadata() {

		switch (getVariant(ServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(OIDSSFGetDynamicTransmitterConfiguration.class, "OIDSSF-6.2");
				break;
			case STATIC:
				callAndStopOnFailure(OIDSSFGetStaticTransmitterConfiguration.class, "OIDSSF-6.2");
				break;
		}

		callAndContinueOnFailure(CheckTransmitterMetadataIssuer.class, Condition.ConditionResult.WARNING, "OIDSSF-6.2");
	}

}
