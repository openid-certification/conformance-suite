package net.openid.conformance.condition.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuerUrlValidation_UnitTest {

	private List<String> validate(String value) {
		List<String> issues = new ArrayList<>();
		IssuerUrlValidation.validate(value, "issuer", issues);
		return issues;
	}

	@Test
	void acceptsPlainHttpsIssuer() {
		assertTrue(validate("https://as.example.com").isEmpty());
	}

	@Test
	void acceptsHttpsIssuerWithPathAndPort() {
		assertTrue(validate("https://as.example.com:8443/tenant1").isEmpty());
	}

	@Test
	void acceptsUppercaseHttpsScheme() {
		// RFC 3986 §3.1: scheme is case-insensitive
		assertTrue(validate("HTTPS://as.example.com").isEmpty());
	}

	@Test
	void rejectsHttpScheme() {
		assertEquals(1, validate("http://as.example.com").size());
	}

	@Test
	void rejectsFragment() {
		List<String> issues = validate("https://as.example.com/x#frag");
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).contains("fragment"));
	}

	@Test
	void rejectsQuery() {
		List<String> issues = validate("https://as.example.com/x?a=b");
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).contains("query"));
	}

	@Test
	void rejectsUserinfo() {
		List<String> issues = validate("https://user@as.example.com");
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).contains("userinfo"));
	}

	@Test
	void rejectsMissingHost() {
		// https with no authority -> no host
		assertTrue(validate("https:///path").stream().anyMatch(i -> i.contains("host")));
	}

	@Test
	void rejectsNonUri() {
		List<String> issues = validate("ht!tp:// not a uri");
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).contains("not a valid URI"));
	}

	@Test
	void reportsMultipleProblemsAtOnce() {
		// http scheme + fragment -> two issues
		List<String> issues = validate("http://as.example.com/x#y");
		assertEquals(2, issues.size());
	}
}
