package net.openid.conformance.vciid2issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.vciid2issuer.condition.VCIFetchCredentialIssuerMetadataSequence;
import net.openid.conformance.vciid2issuer.variant.OID4VCIServerMetadata;

public abstract class AbstractVciTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		JsonObject vciConfig = new JsonObject();
		env.putObject("vci", vciConfig);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected VCIFetchCredentialIssuerMetadataSequence createFetchCredentialIssuerMetadataSequence() {
		return new VCIFetchCredentialIssuerMetadataSequence(getVariant(OID4VCIServerMetadata.class));
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		// No custom configuration
	}

	protected void fetchCredentialIssuerMetadata() {

		call(sequence(this::createFetchCredentialIssuerMetadataSequence));

		exposeEnvString("vci_issuer_url", "vci", "credential_issuer");
		exposeEnvString("vci_metadata_url", "vci","credential_issuer_metadata_url");
	}
}
