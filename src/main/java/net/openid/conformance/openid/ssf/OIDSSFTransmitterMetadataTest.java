package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.openid.ssf.conditions.OIDSSFAuthorizationSchemesTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.OIDSSFDefaultSubjectsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFetchTransmitterConfiguration;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.OIDSSFRequiredFieldsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.OIDSSFSpecVersionTransmitterMetadataCheck;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.SsfDeliveryMode;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-transmitter-metadata",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Metadata",
	summary = "This test verifies the behavior of the transmitter metadata.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix",
	}
)
@VariantParameters({
	SsfDeliveryMode.class,
})
public class OIDSSFTransmitterMetadataTest extends AbstractOIDSSFTest {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		// fetch transmitter metadata
		callAndStopOnFailure(OIDSSFFetchTransmitterConfiguration.class, "OIDSSF-6.2");

		// validate transmitter metadata
		callAndStopOnFailure(OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck.class,"OIDSSF-6.1", "OIDCAEPIOP-2.3.7");
		callAndContinueOnFailure(OIDSSFSpecVersionTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(OIDSSFRequiredFieldsTransmitterMetadataCheck.class);
		callAndStopOnFailure(OIDSSFDefaultSubjectsTransmitterMetadataCheck.class);
		callAndStopOnFailure(OIDSSFAuthorizationSchemesTransmitterMetadataCheck.class, "OIDSSF-6.1.1", "OIDCAEPIOP-2.3.7");


		// populate server jwks
		env.mapKey("server", "transmitter_metadata");

		callAndStopOnFailure(CheckJwksUri.class);
		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		fireTestFinished();
	}
}
