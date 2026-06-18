package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the emulated transmitter in receiver tests advertises only the
 * event families it can generate valid example SETs for. SCIM events (RFC 9967)
 * are recognised in the validation allow-list ({@link SsfEvents#STANDARD_EVENT_TYPES})
 * but must NOT be advertised/generated until real SCIM event examples exist —
 * otherwise a correct receiver could request SCIM events the suite can only
 * deliver as invalid SETs.
 */
public class OIDSSFReceiverEventsSupported_UnitTest {

	/** Minimal concrete subclass forcing the non-CAEP-interop branch. */
	static class TestModule extends AbstractOIDSSFReceiverTestModule {
		@Override
		protected boolean isFinished() {
			return false;
		}

		@Override
		protected boolean isSsfProfileEnabled(SsfProfile profile) {
			return false;
		}
	}

	private final TestModule module = new TestModule();

	@Test
	void advertisedEventsExcludeScim() {
		List<String> advertised = module.getEventsSupported();
		for (String scimEvent : SsfEvents.SCIM_EVENT_TYPES) {
			assertFalse(advertised.contains(scimEvent),
				"SCIM event must not be advertised by the receiver test: " + scimEvent);
		}
	}

	@Test
	void advertisedEventsIncludeGeneratableFamilies() {
		List<String> advertised = module.getEventsSupported();
		assertTrue(advertised.containsAll(SsfEvents.SSF_EVENT_TYPES));
		assertTrue(advertised.containsAll(SsfEvents.CAEP_EVENT_TYPES));
		assertTrue(advertised.containsAll(SsfEvents.RISC_EVENT_TYPES));
	}

	@Test
	void scimRemainsInValidationAllowList() {
		assertTrue(SsfEvents.STANDARD_EVENT_TYPES.containsAll(SsfEvents.SCIM_EVENT_TYPES),
			"SCIM events must remain recognised in the validation allow-list");
	}
}
