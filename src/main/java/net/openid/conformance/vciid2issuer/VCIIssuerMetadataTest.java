package net.openid.conformance.vciid2issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vciid2issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCICheckRequiredMetadataFields;
import net.openid.conformance.vciid2issuer.condition.VCICredentialIssuerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCIEnsureHttpsUrlsMetadata;
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
public class VCIIssuerMetadataTest extends AbstractVciTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);
		eventLog.runBlock("Verify OAuth Authorization Server Metadata", () -> {
			callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-11.2.3", "OID4VCI-11.3");
		});
		eventLog.runBlock("Verify Credential Issuer Metadata", () -> {
			callAndContinueOnFailure(VCICheckRequiredMetadataFields.class, Condition.ConditionResult.FAILURE, "OID4VCI-11.2.3");
			callAndContinueOnFailure(VCIEnsureHttpsUrlsMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-11.2.3");
			callAndContinueOnFailure(VCICredentialIssuerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-11.2.3");
		});

		fireTestFinished();
	}
}
