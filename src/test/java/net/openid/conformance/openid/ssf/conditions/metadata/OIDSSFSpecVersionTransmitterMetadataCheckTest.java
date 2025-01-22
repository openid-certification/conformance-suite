package net.openid.conformance.openid.ssf.conditions.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OIDSSFSpecVersionTransmitterMetadataCheckTest {

	private OIDSSFSpecVersionTransmitterMetadataCheck check = new OIDSSFSpecVersionTransmitterMetadataCheck();

	@Test
	void versionNullShouldBeNotValid() {
		assertFalse(check.isValidVersion(null));
	}

	@Test
	void versionEmptyShouldBeNotValid() {
		assertFalse(check.isValidVersion(""));
	}

	@Test
	void version1dot0ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1.0"));
	}

	@Test
	void version1_0ID1ShouldBeNotValid() {
		assertFalse(check.isValidVersion("1_0-ID1"));
	}

	@Test
	void version1_0ID2ShouldBeValid() {
		assertTrue(check.isValidVersion("1_0-ID2"));
	}

	@Test
	void version1_0ID3ShouldBeValid() {
		assertTrue(check.isValidVersion("1_0-ID3"));
	}

	@Test
	void version1_0ShouldBeValid() {
		assertTrue(check.isValidVersion("1_0"));
	}

	@Test
	void version1_1ShouldBeValid() {
		assertTrue(check.isValidVersion("1_1"));
	}

	@Test
	void version2_0ID1ShouldBeValid() {
		assertTrue(check.isValidVersion("2_0-ID1"));
	}
}
