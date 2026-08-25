package net.openid.conformance.openid.ssf;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers.InvalidSubjectIdentifierException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SsfSubjectIdentifiers_UnitTest {

	private static JsonObject json(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}

	private static void assertInvalid(String json, String expectedMessageFragment) {
		InvalidSubjectIdentifierException e = assertThrows(InvalidSubjectIdentifierException.class,
			() -> SsfSubjectIdentifiers.validate(JsonParser.parseString(json)));
		assertTrue(e.getMessage().contains(expectedMessageFragment),
			"expected message to contain '" + expectedMessageFragment + "' but was: " + e.getMessage());
	}

	// --- RFC 9493 §3 simple formats ---

	@ParameterizedTest
	@ValueSource(strings = {
		"{\"format\":\"account\",\"uri\":\"acct:example.user@service.example.com\"}",
		"{\"format\":\"email\",\"email\":\"user@example.com\"}",
		"{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example.com/\",\"sub\":\"145234573\"}",
		"{\"format\":\"opaque\",\"id\":\"11112222333344445555\"}",
		"{\"format\":\"phone_number\",\"phone_number\":\"+12065550100\"}",
		"{\"format\":\"did\",\"url\":\"did:example:123456\"}",
		"{\"format\":\"did\",\"url\":\"did:example:123456/did/url/path?versionId=1\"}",
		"{\"format\":\"uri\",\"uri\":\"https://user.example.com/\"}",
		"{\"format\":\"jwt_id\",\"iss\":\"https://idp.example.com/123456789/\",\"jti\":\"B70BA622-9515-4353-A866-823539EECBC8\"}",
		"{\"format\":\"saml_assertion_id\",\"issuer\":\"https://idp.example.com/123456789/\",\"assertion_id\":\"_8e8dc5f69a98cc4c1ff3427e5ce34606fd672f91e6\"}",
		"{\"format\":\"ip-addresses\",\"ip-addresses\":[\"10.29.37.75\",\"2001:0db8:0000:0000:0000:8a2e:0370:7334\"]}",
	})
	void shouldAcceptValidSimpleSubjectIdentifiers(String json) {
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(JsonParser.parseString(json)));
	}

	@Test
	void shouldAcceptAliases() {
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(json("""
			{"format":"aliases","identifiers":[
				{"format":"email","email":"user@example.com"},
				{"format":"phone_number","phone_number":"+12065550100"},
				{"format":"email","email":"user+qualifier@example.com"}
			]}""")));
	}

	@Test
	void shouldAcceptProprietaryFormatWithoutMemberChecks() {
		// SSF 1.0 §3.4 allows proprietary formats agreed between the parties
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(json("{\"format\":\"x-vendor-id\",\"anything\":1}")));
	}

	@Test
	void shouldRejectNonObject() {
		assertInvalid("\"user@example.com\"", "must be a JSON object");
		assertThrows(InvalidSubjectIdentifierException.class, () -> SsfSubjectIdentifiers.validate(null));
		assertThrows(InvalidSubjectIdentifierException.class, () -> SsfSubjectIdentifiers.validate(JsonNull.INSTANCE));
	}

	@Test
	void shouldRejectMissingOrInvalidFormat() {
		assertInvalid("{\"email\":\"user@example.com\"}", "missing the required 'format'");
		assertInvalid("{\"format\":\"\",\"email\":\"user@example.com\"}", "'format' member must be a non-empty string");
		assertInvalid("{\"format\":42,\"email\":\"user@example.com\"}", "'format' member must be a non-empty string");
	}

	@Test
	void shouldRejectMissingRequiredMembers() {
		assertInvalid("{\"format\":\"email\"}", "missing the required 'email' member");
		assertInvalid("{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example.com/\"}", "missing the required 'sub' member");
		assertInvalid("{\"format\":\"iss_sub\",\"sub\":\"1234\"}", "missing the required 'iss' member");
		assertInvalid("{\"format\":\"opaque\"}", "missing the required 'id' member");
		assertInvalid("{\"format\":\"phone_number\"}", "missing the required 'phone_number' member");
		assertInvalid("{\"format\":\"did\"}", "missing the required 'url' member");
		assertInvalid("{\"format\":\"uri\"}", "missing the required 'uri' member");
		assertInvalid("{\"format\":\"account\"}", "missing the required 'uri' member");
		assertInvalid("{\"format\":\"jwt_id\",\"iss\":\"https://idp.example.com\"}", "missing the required 'jti' member");
		assertInvalid("{\"format\":\"saml_assertion_id\",\"issuer\":\"https://idp.example.com\"}", "missing the required 'assertion_id' member");
	}

	@Test
	void shouldRejectEmptyOrNonStringRequiredMembers() {
		assertInvalid("{\"format\":\"opaque\",\"id\":\"\"}", "'id' member must be a non-empty string");
		assertInvalid("{\"format\":\"opaque\",\"id\":1234}", "'id' member must be a non-empty string");
		assertInvalid("{\"format\":\"iss_sub\",\"iss\":\"https://issuer.example.com/\",\"sub\":null}", "'sub' member must be a non-empty string");
	}

	@Test
	void shouldRejectMalformedEmail() {
		assertInvalid("{\"format\":\"email\",\"email\":\"not-an-email\"}", "not a valid RFC 5322 addr-spec");
		assertInvalid("{\"format\":\"email\",\"email\":\"@example.com\"}", "not a valid RFC 5322 addr-spec");
		assertInvalid("{\"format\":\"email\",\"email\":\"user@\"}", "not a valid RFC 5322 addr-spec");
		assertInvalid("{\"format\":\"email\",\"email\":\"user name@example.com\"}", "not a valid RFC 5322 addr-spec");
	}

	@Test
	void shouldRejectPhoneNumberWithoutInternationalPrefix() {
		assertInvalid("{\"format\":\"phone_number\",\"phone_number\":\"2065550100\"}", "E.164");
		assertInvalid("{\"format\":\"phone_number\",\"phone_number\":\"+1 206 555 0100\"}", "E.164");
	}

	@Test
	void shouldRejectNonAcctAccountUri() {
		assertInvalid("{\"format\":\"account\",\"uri\":\"https://example.com/user\"}", "'acct' URI");
	}

	@Test
	void shouldRejectNonDidUrl() {
		assertInvalid("{\"format\":\"did\",\"url\":\"https://example.com/did\"}", "must be a DID URL");
	}

	@Test
	void shouldRejectMalformedAliases() {
		assertInvalid("{\"format\":\"aliases\"}", "must contain an 'identifiers' array");
		assertInvalid("{\"format\":\"aliases\",\"identifiers\":[]}", "at least one subject identifier");
		assertInvalid("{\"format\":\"aliases\",\"identifiers\":[{\"format\":\"email\"}]}", "alias identifiers[0]");
		// RFC 9493 §3.8: aliases cannot be nested
		assertInvalid("""
			{"format":"aliases","identifiers":[
				{"format":"aliases","identifiers":[{"format":"email","email":"user@example.com"}]}
			]}""", "nesting of aliases is not allowed");
	}

	@Test
	void shouldRejectMalformedIpAddresses() {
		assertInvalid("{\"format\":\"ip-addresses\"}", "must contain a 'ip-addresses' array");
		assertInvalid("{\"format\":\"ip-addresses\",\"ip-addresses\":[]}", "must not be empty");
		assertInvalid("{\"format\":\"ip-addresses\",\"ip-addresses\":[\"10.0.0.1\", 42]}", "only contain non-empty strings");
	}

	// --- SSF 1.0 §3.3 complex subjects ---

	@Test
	void shouldAcceptComplexSubjectFromSsfSpecExample() {
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(json("""
			{
			  "format": "complex",
			  "user": {"format": "email", "email": "bar@example.com"},
			  "tenant": {"format": "iss_sub", "iss": "https://example.com/idp1", "sub": "1234"}
			}""")));
	}

	@Test
	void shouldAcceptComplexSubjectFromCaepDeviceComplianceExample() {
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(json("""
			{
			  "format": "complex",
			  "device": {"format": "iss_sub", "iss": "https://idp.example.com/123456789/", "sub": "e9297990-14d2-42ec-a4a9-4036db86509a"},
			  "tenant": {"format": "opaque", "id": "123456789"}
			}""")));
	}

	@Test
	void shouldAcceptComplexSubjectWithAdditionalMemberNames() {
		// SSF 1.0 §3.3: additional Subject Member names MAY be used
		assertDoesNotThrow(() -> SsfSubjectIdentifiers.validate(json("""
			{"format":"complex","workload":{"format":"opaque","id":"wl-1"}}""")));
	}

	@Test
	void shouldRejectComplexSubjectWithoutMembers() {
		assertInvalid("{\"format\":\"complex\"}", "at least one Subject Member");
	}

	@Test
	void shouldRejectComplexSubjectWithInvalidMember() {
		assertInvalid("{\"format\":\"complex\",\"user\":{\"format\":\"email\"}}", "Subject Member 'user'");
		assertInvalid("{\"format\":\"complex\",\"user\":\"bar@example.com\"}", "Subject Member 'user' must be a JSON object");
	}

	@Test
	void shouldRejectNestedComplexSubject() {
		assertInvalid("""
			{"format":"complex","user":{"format":"complex","user":{"format":"email","email":"bar@example.com"}}}""",
			"nesting of Complex Subjects is not allowed");
	}

	// --- helpers ---

	@Test
	void shouldExposeFormatAndComplexMembers() {
		JsonObject complex = json("""
			{
			  "format": "complex",
			  "user": {"format": "email", "email": "bar@example.com"},
			  "tenant": {"format": "opaque", "id": "1234"}
			}""");
		assertEquals("complex", SsfSubjectIdentifiers.getFormat(complex));
		assertTrue(SsfSubjectIdentifiers.isComplex(complex));

		Map<String, ?> members = SsfSubjectIdentifiers.getComplexSubjectMembers(complex);
		assertEquals(2, members.size());
		assertTrue(members.containsKey("user"));
		assertTrue(members.containsKey("tenant"));
		assertFalse(members.containsKey("format"));

		JsonObject simple = json("{\"format\":\"email\",\"email\":\"bar@example.com\"}");
		assertEquals("email", SsfSubjectIdentifiers.getFormat(simple));
		assertFalse(SsfSubjectIdentifiers.isComplex(simple));
		assertTrue(SsfSubjectIdentifiers.getComplexSubjectMembers(simple).isEmpty());

		assertNull(SsfSubjectIdentifiers.getFormat(null));
		assertNull(SsfSubjectIdentifiers.getFormat(new JsonPrimitive("email")));
		assertNull(SsfSubjectIdentifiers.getFormat(json("{\"format\":1}")));
	}

	@Test
	void caepInteropFormatSetsMatchProfileSection25() {
		assertEquals(java.util.Set.of("email", "iss_sub"), SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS);
		assertEquals(java.util.Set.of("opaque"), SsfSubjectIdentifiers.CAEP_INTEROP_VERIFICATION_SUBJECT_FORMATS);
	}
}
