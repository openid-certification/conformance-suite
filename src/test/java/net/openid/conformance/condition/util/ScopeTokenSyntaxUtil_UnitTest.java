package net.openid.conformance.condition.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScopeTokenSyntaxUtil_UnitTest {

	@Test
	void acceptsTypicalScopeTokens() {
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken("openid"));
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken("profile"));
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken("offline_access"));
	}

	@Test
	void acceptsKommPassStyleScope() {
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken("dc+sd-jwt:https://dresden.de/credentials/KommPass"));
	}

	@Test
	void acceptsAllVisibleAsciiExceptForbidden() {
		StringBuilder allAllowed = new StringBuilder();
		allAllowed.append((char) 0x21);
		for (int c = 0x23; c <= 0x5B; c++) {
			allAllowed.append((char) c);
		}
		for (int c = 0x5D; c <= 0x7E; c++) {
			allAllowed.append((char) c);
		}
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken(allAllowed.toString()));
	}

	@Test
	void rejectsEmpty() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken(""));
	}

	@Test
	void rejectsNull() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken(null));
	}

	@Test
	void rejectsTokenWithSpace() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("read write"));
	}

	@Test
	void rejectsTokenWithDoubleQuote() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("foo\"bar"));
	}

	@Test
	void rejectsTokenWithBackslash() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("foo\\bar"));
	}

	@Test
	void rejectsTokenWithControlChar() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("foo\tbar"));
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("foo\nbar"));
	}

	@Test
	void rejectsTokenWithNonAscii() {
		assertFalse(ScopeTokenSyntaxUtil.isValidScopeToken("café"));
	}

	@Test
	void underscoreIsPermitted() {
		// 0x5F is inside the %x5D-5F range; underscore is a valid scope-token character
		assertTrue(ScopeTokenSyntaxUtil.isValidScopeToken("offline_access"));
		assertNull(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("offline_access"));
	}

	@Test
	void syntaxErrorIsNullForValidToken() {
		assertNull(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("openid"));
	}

	@Test
	void syntaxErrorNamesTheOffendingCharacterAndPosition() {
		String err = ScopeTokenSyntaxUtil.scopeTokenSyntaxError("two words");
		assertEquals("contains ' ' (0x20 SPACE) at index 3, which is not permitted by the RFC 6749 Appendix A.4 "
			+ "scope-token ABNF (allowed characters are %x21, %x23-5B and %x5D-7E: visible ASCII "
			+ "except space, double-quote and backslash)", err);
	}

	@Test
	void syntaxErrorNamesDoubleQuoteAndBackslash() {
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("fo\"o").contains("(0x22 DQUOTE)"));
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("fo\\o").contains("(0x5C BACKSLASH)"));
	}

	@Test
	void syntaxErrorDescribesControlAndNonAscii() {
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("foo\tbar").contains("control character"));
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("café").contains("non-ASCII character"));
	}

	@Test
	void syntaxErrorForEmptyMentionsEmptiness() {
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError("").contains("empty"));
		assertTrue(ScopeTokenSyntaxUtil.scopeTokenSyntaxError(null).contains("empty"));
	}
}
