package net.openid.conformance.vciid2issuer;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "oid4vci-id2-issuer-happy-flow",
	displayName = "OID4VCIID2: Happy flow test",
	summary = "Expects the issuer to issue a verifiable credential according to the OID4VCI specification.",
	profile = "OID4VCI-ID2",
	configurationFields = { //
		"server.discoveryIssuer", //

	}
)
@VariantParameters({//
	ClientAuthType.class, //
	ClientRegistration.class,
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = { //
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = { //
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = { //
	"client.client_id"
})
public class VCIIssuerHappyPathTest extends AbstractVciTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Credential Issuer Metadata", this::fetchCredentialIssuerMetadata);

		// obtain USER_ACCESS_TOKEN

		// TODO derive credential-offer-uri from issuer or make it configurable

		// prepare authorization request for PRE_AUTHORIZED_CODE (credential-offer-uri, USER_ACCESS_TOKEN)
		// obtain PRE_AUTHORIZED_CODE

		// obtain credential access token with PRE_AUTHORIZED_CODE
		/*
		CREDENTIAL_BEARER_TOKEN=$(curl -k -s $KEYCLOAK_EXTERNAL_ADDR/realms/$KEYCLOAK_REALM/protocol/openid-connect/token \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code' \
    -d "pre-authorized_code=$PRE_AUTHORIZED_CODE" \
    -d "client_id=openid4vc-rest-api" \
    -d "client_secret=$CLIENT_SECRET")
		 */

		// validate access token response with credential access token

		// prepare request with proof

		// Obtain the credential

		// verify credential

		fireTestFinished();
	}
}
