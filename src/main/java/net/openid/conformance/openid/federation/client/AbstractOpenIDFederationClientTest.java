package net.openid.conformance.openid.federation.client;

import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"federation.entity_configuration"
})
public abstract class AbstractOpenIDFederationClientTest extends AbstractTestModule {
}
