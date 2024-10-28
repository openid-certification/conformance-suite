package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12OrLater;
import net.openid.conformance.condition.common.EnsureTLS13OrLater;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGetDynamicTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFGetStaticTransmitterConfiguration;
import net.openid.conformance.openid.ssf.conditions.OIDSSFObtainTransmitterAccessToken;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
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
	}

	protected void obtainTransmitterAccessToken() {
		switch (getVariant(SsfAuthMode.class)) {
			case STATIC:
				callAndStopOnFailure(OIDSSFObtainTransmitterAccessToken.class);
				break;
			case DYNAMIC:
				// TODO fetch token via client credentials grant
				callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

				break;
		}

	}

	protected void validateTlsConnection() {
		callAndContinueOnFailure(EnsureTLS12OrLater.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(EnsureTLS13OrLater.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
	}
}
