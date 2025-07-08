package net.openid.conformance.vciid2issuer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsAttestJwtClientAuth;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.vciid2issuer.condition.VCIAuthorizationServerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCICheckRequiredMetadataFields;
import net.openid.conformance.vciid2issuer.condition.VCICredentialIssuerMetadataValidation;
import net.openid.conformance.vciid2issuer.condition.VCIEnsureHttpsUrlsMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIFetchOAuthorizationServerMetadata;
import net.openid.conformance.vciid2issuer.condition.VCIValidateCredentialIssuerUri;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-metadata-test",
	displayName = "OID4VCIID2: Issuer metadata test",
	summary = "This test case validates the metadata exposed by the credential issuer, as defined in the OpenID for Verifiable Credential Issuance (OpenID4VCI) specification.",
	profile = "OID4VCI-ID2",
	configurationFields = {
		"server.discoveryIssuer"
	}
)
@VariantParameters({VCIServerMetadata.class, VCIID2ClientAuthType.class})
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
				String authServerMetadataPath = String.format("authorization_servers.server%d.authorization_server_metadata", 0);
				checkAuthServerMetadata(authServerMetadataPath);
			} else {
				JsonArray authServers = authorizationServersEL.getAsJsonArray();
				for (int i = 0; i < authServers.size(); i++) {
					String authServerMetadataPath = String.format("authorization_servers.server%d.authorization_server_metadata", i);
					checkAuthServerMetadata(authServerMetadataPath);
				}
			}
		});

		fireTestFinished();
	}

	protected void checkAuthServerMetadata(String authServerMetadataPath) {
		env.runWithMapKey("current_auth_server_metadata_path", authServerMetadataPath, () -> {
			callAndStopOnFailure(VCIAuthorizationServerMetadataValidation.class, Condition.ConditionResult.FAILURE, "OID4VCI-ID2-11.2.3", "OID4VCI-ID2-11.3");

			if (clientAuthType == VCIID2ClientAuthType.CLIENT_ATTESTATION) {
				env.putObject("server", env.getElementFromObject("vci", authServerMetadataPath).getAsJsonObject());
				callAndContinueOnFailure(EnsureServerConfigurationSupportsAttestJwtClientAuth.class, Condition.ConditionResult.WARNING, "OAuth2-ATCA05-12.2");
				env.removeObject("server");
			}
		});
	}
}
