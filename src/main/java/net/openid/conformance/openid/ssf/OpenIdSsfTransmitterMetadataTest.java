package net.openid.conformance.openid.ssf;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-transmitter-metadata",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Metadata",
	summary = "This test verifies the behavior of the transmitter metadata. ",
	profile = "OIDSSF",
	configurationFields = {
	}
)
public class OpenIdSsfTransmitterMetadataTest extends AbstractOpenIdSsfTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// fetch transmitter metadata

		// apply metadata checks
		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html

		//OID_CAEP_INTEROP-2.1 TLS 1.2 or later with unrevoked, valid certificate

		//OID_CAEP_INTEROP-2.2 ID-2 (or later) format events

		//OID_CAEP_INTEROP-2.3.1 Spec version 1_0-ID2 or greater spec_version

		//OID_CAEP_INTEROP-2.3.2 Delivery method delivery_methods_supported

		//OID_CAEP_INTEROP-2.3.3 Fetch / Check JWKS URI ("verify" current signing key)

		//OID_CAEP_INTEROP-2.3.4 Check Configuration endpoint configuration_endpoint
		// ensure POST method works

		//OID_CAEP_INTEROP-2.3.6 Verification endpoint verification_endpoint

		//OID_CAEP_INTEROP-2.3.7 Authorization schemes, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-03.html#name-authorization-scheme

		//OID_CAEP_INTEROP-2.3.8.1 Verify deliver method: Push or Poll delivery
		// urn:ietf:rfc:8935 (Push)
		// urn:ietf:rfc:8936 (Poll)



		//OID_CAEP_INTEROP-2.5 Subject formats

		// 2.6 Signed events


		fireTestFinished();
	}

}
