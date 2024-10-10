package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-receiver-subject-control",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Subject Control",
	summary = "This test verifies the behavior of the receiver subject control.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
		"ssf.transmitter.access_token"
	}
)
@VariantParameters({
	ServerMetadata.class,
	SsfDeliveryMode.class,
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value="static", configurationFields = {
	"ssf.transmitter.configuration_metadata_endpoint",
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value="discovery", configurationFields = {
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix",
})
public class OIDSSFReceiverSubjectControlTest extends AbstractOIDSSFTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// ensure stream exists
		// add subject with subjectid formats:
		// - email
		// - iss_sub
		// - opaque (for the Verification event only)

		// remove subject(s)

		fireTestFinished();
	}

}
