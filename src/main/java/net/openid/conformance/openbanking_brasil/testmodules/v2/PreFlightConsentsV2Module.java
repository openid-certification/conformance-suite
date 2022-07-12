package net.openid.conformance.openbanking_brasil.testmodules.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.MapDirectoryValues;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.openbanking_brasil.testmodules.support.UnmapDirectoryValues;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2.ValidateConsentsFieldV2;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "preflight-consents-v2",
	displayName = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. An SSA will be generated using the Open Banking Brasil Directory. Finally" +
		"a check of necessaru fields will be made",
	summary = "Pre-flight checks will validate the mTLS certificate before requesting an access token using the Directory client_id provided in the test configuration. An SSA will be generated using the Open Banking Brasil Directory. Finally" +
		"a check of necessaru fields will be made",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
        "directory.client_id"
	}
)

public class PreFlightConsentsV2Module extends AbstractClientCredentialsGrantFunctionalTestModule {

    @Override
    protected void runTests() {
        runInBlock("Pre-flight MTLS Cert Checks", () -> {
            callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		    callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

            // normally our DCR tests create a key on the fly to use, but in this case the key has to be registered
            // manually with the central directory so we must use user supplied keys
            callAndStopOnFailure(ExtractJWKSDirectFromClientConfiguration.class);

            callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
        });

        runInBlock("Pre-flight Get an SSA", () -> {

			env.mapKey("access_token", "directory_access_token");

			callAndStopOnFailure(SetDirectoryInfo.class);
            callAndStopOnFailure(ExtractDirectoryConfiguration.class);

		    callAndContinueOnFailure(FAPIBrazilCheckDirectoryDiscoveryUrl.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");

		    callAndContinueOnFailure(FAPIBrazilCheckDirectoryApiBase.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-1");

            callAndStopOnFailure(MapDirectoryValues.class);

            callAndStopOnFailure(GetDynamicServerConfiguration.class);

            // this overwrites the non-directory values; we will have to replace them below
            callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");

            callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

            callAndStopOnFailure(SetDirectorySoftwareScopeOnTokenEndpointRequest.class);

            // MTLS client auth
            callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

            callAndStopOnFailure(CallTokenEndpoint.class);

            callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

            callAndStopOnFailure(CheckForAccessTokenValue.class);

            callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

            callAndStopOnFailure(UnmapDirectoryValues.class);

            // restore MTLS aliases to the values for the server being tested
            callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");

            callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);

            // use access token to get ssa
            // https://matls-api.sandbox.directory.openbankingbrasil.org.br/organisations/${ORGID}/softwarestatements/${SSID}/assertion
            callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);

			env.unmapKey("access_token");
		});

		runInBlock("Pre-flight Consent field checks", () -> {
			callAndContinueOnFailure(ValidateConsentsFieldV2.class, Condition.ConditionResult.FAILURE);
		});
    }
}
