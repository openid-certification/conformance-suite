package net.openid.conformance.openid.ssf;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-transmitter-subject-control",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Subject Control",
	summary = "This test verifies the behavior of the transmitter subject control.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
		"ssf.transmitter.access_token"
	}
)
public class OIDSSFTransmitterSubjectControlTest extends AbstractOIDSSFTest {

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
