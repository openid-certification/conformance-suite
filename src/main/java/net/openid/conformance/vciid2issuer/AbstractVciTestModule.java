package net.openid.conformance.vciid2issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.vciid2issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIGetDynamicCredentialIssuerMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIGetStaticCredentialIssuerMetadata;
import net.openid.conformance.vciid2issuer.variant.OID4VCIServerMetadata;

public abstract class AbstractVciTestModule extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

	}

	protected void fetchCredentialIssuerMetadata() {

		switch (getVariant(OID4VCIServerMetadata.class)) {
			case DISCOVERY:
				callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-ID2-11.2.2");
				break;
			case STATIC:
				callAndStopOnFailure(VCIGetStaticCredentialIssuerMetadata.class, "OID4VCI-ID2-11.2.2");
				break;
		}

		callAndContinueOnFailure(VCIFetchOAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");

		exposeEnvString("vci_metadata_url", "vci","credential_issuer_metadata_url");
	}
}
