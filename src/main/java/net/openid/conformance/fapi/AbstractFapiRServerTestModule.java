package net.openid.conformance.fapi;

import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FapiRClientAuthType;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

@VariantParameters({
	FapiRClientAuthType.class,
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg",
	"client2.client_id", // This is because the 'none' variant doesn't use the second client
	"client2.scope",
	"client2.client_secret",
	"client2.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks",
	"client2.client_id",
	"client2.scope",
	"client2.jwks"
})
@VariantConfigurationFields(parameter = FapiRClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca",
	"client2.client_id",
	"client2.scope",
	"mtls2.key",
	"mtls2.cert",
	"mtls2.ca"
})
public abstract class AbstractFapiRServerTestModule extends AbstractRedirectServerTestModule {
}
