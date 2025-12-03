package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly;
import net.openid.conformance.openid.federation.AbstractOpenIDFederationTest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretBasic;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.variant.FederationEntityMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

@VariantParameters({
	FederationEntityMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = FederationEntityMetadata.class, value = "static", configurationFields = {
	"federation.entity_configuration",
})
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"explicit"})
public abstract class AbstractOpenIDFederationClientTest extends AbstractOpenIDFederationTest {

	protected Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	protected Class<? extends ConditionSequence> validateClientAuthenticationSteps;

	@VariantSetup(parameter = ClientRegistration.class, value = "automatic")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	// TODO: Fix this when explicit registration tests are added
	@VariantSetup(parameter = ClientRegistration.class, value = "explicit")
	public void setupClientSecret() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretBasic.class;
	}

}
