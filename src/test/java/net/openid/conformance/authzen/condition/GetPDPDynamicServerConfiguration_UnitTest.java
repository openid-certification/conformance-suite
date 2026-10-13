package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the well-known discovery URL derivation (the
 * {@code /.well-known/authzen-configuration} segment is inserted between the host
 * and any existing path, not appended after the whole URL) and the parsing of the
 * metadata response body.
 */
class GetPDPDynamicServerConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GetPDPDynamicServerConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new GetPDPDynamicServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void noPath_appendsWellKnown() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration",
			cond.deriveDiscoveryUrl("https://pdp.example.com"));
	}

	@Test
	public void trailingSlashOnly_appendsWellKnown() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration",
			cond.deriveDiscoveryUrl("https://pdp.example.com/"));
	}

	@Test
	public void tenantPath_insertsWellKnownBeforePath() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com/tenant1"));
	}

	@Test
	public void tenantPathTrailingSlash_insertsWellKnownAndStripsTrailingSlash() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com/tenant1/"));
	}

	@Test
	public void nestedPath_insertsWellKnownBeforeFullPath() {
		assertEquals("https://pdp.example.com/.well-known/authzen-configuration/a/b",
			cond.deriveDiscoveryUrl("https://pdp.example.com/a/b"));
	}

	@Test
	public void hostWithPort_isPreserved() {
		assertEquals("https://pdp.example.com:8443/.well-known/authzen-configuration/tenant1",
			cond.deriveDiscoveryUrl("https://pdp.example.com:8443/tenant1"));
	}

	@Test
	public void invalidUrl_fails() {
		assertThrows(ConditionError.class, () -> cond.deriveDiscoveryUrl("ht tp://bad url"));
	}

	@Test
	public void relativeUrlWithoutSchemeOrHost_fails() {
		assertThrows(ConditionError.class, () -> cond.deriveDiscoveryUrl("/tenant1"));
	}
	@Test
	public void objectBody_isParsed() {
		assertEquals("https://pdp.example.com",
			OIDFJSON.getString(cond.parseServerConfiguration("{\"policy_decision_point\":\"https://pdp.example.com\"}").get("policy_decision_point")));
	}

	@Test
	public void arrayBody_fails() {
		Throwable e = assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("[]"));
		assertTrue(e.getMessage().contains("not a JSON object"));
	}

	@Test
	public void stringBody_fails() {
		Throwable e = assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("\"metadata\""));
		assertTrue(e.getMessage().contains("not a JSON object"));
	}

	@Test
	public void malformedBody_fails() {
		assertThrows(ConditionError.class, () -> cond.parseServerConfiguration("{"));
	}

	@Test
	public void emptyBody_fails() {
		assertThrows(ConditionError.class, () -> cond.parseServerConfiguration(""));
	}
}
