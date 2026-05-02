package net.openid.conformance.vci10issuer;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.ConfigurationFields;
import net.openid.conformance.vci10issuer.condition.VCIGetDynamicCredentialIssuerMetadata;

@ConfigurationFields({"vci.credential_issuer_url"})
public abstract class AbstractVciTest extends AbstractTestModule {

	protected ClientAuthType clientAuthType;

	protected FAPI2FinalOPProfile fapi2Profile;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putString("external_url_override", externalUrlOverride);
		env.putObject("config", config);

		JsonObject vciConfig = new JsonObject();
		env.putObject("vci", vciConfig);

		clientAuthType = getVariant(ClientAuthType.class);
		fapi2Profile = getVariant(FAPI2FinalOPProfile.class);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		// No custom configuration
	}

	protected boolean isHaip() {
		return fapi2Profile == FAPI2FinalOPProfile.VCI_HAIP;
	}

	protected void fetchCredentialIssuerMetadata() {

		callAndStopOnFailure(VCIGetDynamicCredentialIssuerMetadata.class, "OID4VCI-1FINAL-12.2.2");

		exposeEnvString("vci_metadata_url", "vci","credential_issuer_metadata_url");
	}
}
