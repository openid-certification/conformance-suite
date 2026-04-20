package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-caep-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final/CAEP Interop Profile: Transmitter (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	summary = """
		Collection of tests to verify the behavior of an OpenID Shared Signals Framework Transmitter
		against the CAEP Interop Profile 1.0.

		This plan exercises the following transmitter behavior:
		 * transmitter metadata document (required and optional fields, authorization schemes, advertised delivery methods and event types)
		 * stream configuration management: create, read, delete (update/replace are excluded per CAEPIOP 2.3.8 which restricts stream configuration lifecycle operations)
		 * stream configuration error handling: invalid access tokens, malformed bodies, unknown stream IDs
		 * stream verification via the transmitter's verification endpoint
		 * end-to-end CAEP Interop event delivery (session-revoked, credential-change, device-compliance-change — as advertised in events_delivered) over PUSH and POLL

		Each CAEP event received during the interop run is validated against the CAEP 1.0 Final
		specification (required claims, event-type-specific fields, signature, SET envelope).
		""",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFTransmitterTestPlanCaepInterop implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		var testModules = new ArrayList<>(OIDSSFTransmitterTestPlan.testModules);

		testModules.add(OIDSSFTransmitterStreamCaepInteropTest.class);

		testModules.removeAll(List.of(
			OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidToken.class,
			OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidBody.class,
			OIDSSFStreamControlNegativeTestUpdateUnknownStream.class,
			OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidBody.class,
			OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidToken.class,
			OIDSSFStreamControlNegativeTestReplaceUnknownStream.class,
			OIDSSFStreamSubjectControlHappyPathTest.class
		));

		return List.of(new ModuleListEntry(testModules, List.of(
			new Variant(SsfProfile.class, SsfProfile.CAEP_INTEROP)
		)));
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variants) {
		return List.of("OpenID SSF Transmitter 1.0 Final+CAEP-Interop-1.0-ID2");
	}

}
