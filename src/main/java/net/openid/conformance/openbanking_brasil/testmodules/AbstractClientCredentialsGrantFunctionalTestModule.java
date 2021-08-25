package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs;
import net.openid.conformance.condition.common.FAPIBrazilCheckKeyAlgInClientJWKs;
import net.openid.conformance.condition.common.FAPICheckKeyAlgInClientJWKs;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.ObtainAccessTokenWithClientCredentials;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.*;
import net.openid.conformance.variant.*;

@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {"openbanking_uk", "plain_fapi", "consumerdataright_au"})
@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	FAPIResponseMode.class
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk", configurationFields = {
	"resource.resourceUrlAccountRequests",
	"resource.resourceUrlAccountsResource"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "consumerdataright_au", configurationFields = {
	"resource.cdrVersion"
})
@VariantConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"resource.consentUrl",
	"resource.brazilCpf",
	"resource.brazilCnpj"
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractClientCredentialsGrantFunctionalTestModule extends AbstractBlockLoggingTestModule {

	protected Class<? extends ConditionSequence> clientAuthSequence;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		preConfigure(config, baseUrl, externalUrlOverride);
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)));
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		postConfigure(config, baseUrl, externalUrlOverride);
		setStatus(Status.CONFIGURED);
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		runTests();
		fireTestFinished();
	}

	protected abstract void runTests();


	protected void preConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {

	}

	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {

	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		clientAuthSequence = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		clientAuthSequence = CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class;
	}

}
