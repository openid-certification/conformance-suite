package net.openid.conformance.vciid2issuer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vciid2issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCICheckRequiredMetadataFields;
import net.openid.conformance.vciid2issuer.condition.VCICredentialIssuerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCIEnsureHttpsUrlsMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIValidateCredentialIssuerUri;
import net.openid.conformance.variant.VCIServerMetadata;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-metadata-test",
	displayName = "OID4VCIID2: Issuer metadata test",
	summary = "Expects the issuer to expose the credential issuer metadata according to OID4VCI specification.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.discoveryIssuer"
	}
)
@VariantParameters({VCIServerMetadata.class,})
@VariantConfigurationFields(parameter = VCIServerMetadata.class, value = "static", configurationFields = {"vci.credential_issuer_metadata_url",})
public class VCIIssuerMetadataTest extends AbstractVciTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);

		eventLog.runBlock("Verify Credential Issuer Metadata", () -> {
			callAndContinueOnFailure(VCICheckRequiredMetadataFields.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
			callAndContinueOnFailure(VCIEnsureHttpsUrlsMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
			callAndContinueOnFailure(VCIValidateCredentialIssuerUri.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.1");
			callAndContinueOnFailure(VCICredentialIssuerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3");
		});

		eventLog.runBlock("Fetch OAuth Authorization Server Metadata", () -> {
			callAndStopOnFailure(VCIFetchOAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3", "RFC8414-3.1");
		});

		eventLog.runBlock("Verify OAuth Authorization Server Metadata", () -> {

			JsonObject credentialIssuerMetadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
			JsonElement authorizationServersEL = credentialIssuerMetadata.get("authorization_servers");
			if (authorizationServersEL == null) {
				callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3", "OID4VCI-ID2-11.3");
			} else {
				JsonArray authServers = authorizationServersEL.getAsJsonArray();
				for (int i = 0; i < authServers.size(); i++) {
					callAndStopOnFailure(new VCIAuthorizationServerMetadataValidation(i), Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3", "OID4VCI-ID2-11.3");
				}
			}
		});

		fireTestFinished();
	}
}
