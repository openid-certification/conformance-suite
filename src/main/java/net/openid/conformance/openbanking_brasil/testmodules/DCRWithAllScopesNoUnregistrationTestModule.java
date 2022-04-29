package net.openid.conformance.openbanking_brasil.testmodules;


import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "dcr-with-all-scopes-no-unregistration",
	displayName = "Dynamic Client Registration with the full set of scopes without unregistering the client after the test completion. ",
	summary = "Dynamic Client Registration with the full set of scopes without unregistering the client after the test completion.\n" +
		"\u2022 Using the client provided on the configuration retrieves from the directory its SSA\n" +
		"\u2022 Performs a DCR on the authorization server provided on the configuration using all the existing Phase 2 and 3 scopes\n" +
		"\u2022 Displays 'client_id' and does not unregister the client after the test completion",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client.jwks"
	}
)

@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"resource.consentUrl",
	"resource.brazilCpf",
	"resource.brazilCnpj",
	"client.org_jwks"
})
public class DCRWithAllScopesNoUnregistrationTestModule extends AbstractApiDcrTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);

		callAndStopOnFailure(SetDirectoryInfo.class);

		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);
		String clientAuth = clientAuthType.toString();
		if (clientAuthType == ClientAuthType.MTLS) {
			// the FAPI auth type variant doesn't use the name required for the registration request as per
			// https://datatracker.ietf.org/doc/html/rfc8705#section-2.1.1
			clientAuth = "tls_client_auth";
		}
		env.putString("client_auth_type", clientAuth);

		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		// normally our DCR tests create a key on the fly to use, but in this case the key has to be registered
		// manually with the central directory so we must use user supplied keys
		callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);

		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");

		getSsa();


	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		eventLog.startBlock("Perform Dynamic Client Registration");

		callAndStopOnFailure(StoreOriginalClientConfiguration.class);
		callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);

		setupJwksUri();

		// create basic dynamic registration request
		callAndStopOnFailure(CreateEmptyDynamicRegistrationRequest.class);

		callAndStopOnFailure(AddAuthorizationCodeGrantTypeToDynamicRegistrationRequest.class);
		if (!jarm.isTrue()) {
			// implicit is only required when id_token is returned in frontchannel
			callAndStopOnFailure(AddImplicitGrantTypeToDynamicRegistrationRequest.class);
		}
		callAndStopOnFailure(AddRefreshTokenGrantTypeToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddClientCredentialsGrantTypeToDynamicRegistrationRequest.class);


		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);

		if (clientAuthType == ClientAuthType.MTLS) {
			addTlsClientAuthSubjectDn();
		}

		addJwksToRequest();
		callAndStopOnFailure(AddTokenEndpointAuthMethodToDynamicRegistrationRequestFromEnvironment.class);
		if (jarm.isTrue()) {
			callAndStopOnFailure(SetResponseTypeCodeInDynamicRegistrationRequest.class);
		} else {
			callAndStopOnFailure(SetResponseTypeCodeIdTokenInDynamicRegistrationRequest.class);
		}
		validateSsa();
		callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

		addSoftwareStatementToRegistrationRequest();

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));

		eventLog.endBlock();

		fireTestFinished();

	}

	@Override
	protected void setupResourceEndpoint() {}

	@Override
	public void cleanup() {}
}
