package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.federation.AbstractOpenIDFederationTest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"federation.entity_configuration",
})
public abstract class AbstractOpenIDFederationClientTest extends AbstractOpenIDFederationTest {

	protected Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	protected Class<? extends ConditionSequence> validateClientAuthenticationSteps;

	@VariantSetup(parameter = ClientRegistration.class, value = "automatic")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
		//clientRegistrationSteps = OIDCCRegisterClientWithPrivateKeyJwt.class;
	}

}
