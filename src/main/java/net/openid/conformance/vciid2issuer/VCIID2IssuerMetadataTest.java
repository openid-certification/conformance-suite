package net.openid.conformance.vciid2issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vciid2issuer.condition.VCIID2EnsureHttpsUrlsMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIID2CheckRequiredMetadataFields;
import net.openid.conformance.vciid2issuer.condition.VICID2CheckCredentialConfigurationsSupported;
import net.openid.conformance.vciid2issuer.variant.OID4VCIServerMetadata;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-metadata-test",
	displayName = "OID4VCIID2: Issuer metadata test",
	summary = "Expects the issuer to expose the credential issuer metadata according to OID4VCI specification.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.discoveryIssuer"
	}
)
@VariantParameters({OID4VCIServerMetadata.class,})
@VariantConfigurationFields(parameter = OID4VCIServerMetadata.class, value = "static", configurationFields = {"vci.credential_issuer_metadata_url",})
public class VCIID2IssuerMetadataTest extends AbstractVciId2TestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);
		eventLog.runBlock("Verify Credential Issuer Metadata", () -> {
			callAndContinueOnFailure(VCIID2CheckRequiredMetadataFields.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
			callAndContinueOnFailure(VCIID2EnsureHttpsUrlsMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
			callAndContinueOnFailure(VICID2CheckCredentialConfigurationsSupported.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
		});

		fireTestFinished();
	}
}
