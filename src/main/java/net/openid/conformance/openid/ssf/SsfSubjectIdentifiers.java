package net.openid.conformance.openid.ssf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Subject identifier formats and structural validation for SSF events.
 * <p>
 * Covers the formats registered by
 * <a href="https://www.rfc-editor.org/rfc/rfc9493.html#section-3">RFC 9493 §3</a>,
 * the additional formats defined in
 * <a href="https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-3.5">SSF 1.0 §3.5</a>
 * and the Complex Subject structure of
 * <a href="https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-3.3">SSF 1.0 §3.3</a>.
 * Formats not known to this class are accepted as proprietary formats (SSF 1.0 §3.4) — only
 * the presence of a non-empty {@code format} member is checked for them.
 * <p>
 * The CAEP Interoperability Profile restricts the formats that may appear on CAEP events, see
 * <a href="https://openid.github.io/sharedsignals/openid-caep-interoperability-profile-1_0.html#section-2.5">CAEPIOP §2.5</a>:
 * {@code email} and {@code iss_sub} MUST be supported, {@code opaque} only for the Verification event.
 */
public final class SsfSubjectIdentifiers {

	// RFC 9493 §3
	public static final String FORMAT_ACCOUNT = "account";
	public static final String FORMAT_EMAIL = "email";
	public static final String FORMAT_ISS_SUB = "iss_sub";
	public static final String FORMAT_OPAQUE = "opaque";
	public static final String FORMAT_PHONE_NUMBER = "phone_number";
	public static final String FORMAT_DID = "did";
	public static final String FORMAT_URI = "uri";
	public static final String FORMAT_ALIASES = "aliases";

	// SSF 1.0 §3.5
	public static final String FORMAT_JWT_ID = "jwt_id";
	public static final String FORMAT_SAML_ASSERTION_ID = "saml_assertion_id";
	public static final String FORMAT_IP_ADDRESSES = "ip-addresses";

	// SSF 1.0 §3.3
	public static final String FORMAT_COMPLEX = "complex";

	/**
	 * Subject Member names defined for Complex Subjects in SSF 1.0 §3.3. Additional member
	 * names MAY be used, so this set is informational only.
	 */
	public static final Set<String> COMPLEX_SUBJECT_MEMBER_NAMES = Set.of(
		"user", "device", "session", "application", "tenant", "org_unit", "group");

	/**
	 * Formats a CAEP event {@code sub_id} MUST use under CAEPIOP §2.5.
	 */
	public static final Set<String> CAEP_INTEROP_EVENT_SUBJECT_FORMATS = Set.of(FORMAT_EMAIL, FORMAT_ISS_SUB);

	/**
	 * Format the Verification event {@code sub_id} uses under CAEPIOP §2.5 / SSF 1.0 §8.1.4.1.
	 */
	public static final Set<String> CAEP_INTEROP_VERIFICATION_SUBJECT_FORMATS = Set.of(FORMAT_OPAQUE);

	/**
	 * Required string members per known simple format. Formats whose members are not plain
	 * strings ({@code aliases}, {@code ip-addresses}, {@code complex}) are handled explicitly.
	 */
	private static final Map<String, List<String>> REQUIRED_STRING_MEMBERS = Map.ofEntries(
		Map.entry(FORMAT_ACCOUNT, List.of("uri")),
		Map.entry(FORMAT_EMAIL, List.of("email")),
		Map.entry(FORMAT_ISS_SUB, List.of("iss", "sub")),
		Map.entry(FORMAT_OPAQUE, List.of("id")),
		Map.entry(FORMAT_PHONE_NUMBER, List.of("phone_number")),
		Map.entry(FORMAT_DID, List.of("url")),
		Map.entry(FORMAT_URI, List.of("uri")),
		Map.entry(FORMAT_JWT_ID, List.of("iss", "jti")),
		Map.entry(FORMAT_SAML_ASSERTION_ID, List.of("issuer", "assertion_id"))
	);

	/**
	 * RFC 5322 addr-spec sanity check: exactly one {@code @}, non-empty local part and domain,
	 * no whitespace. Deliberately lenient — full RFC 5322 parsing is out of scope.
	 */
	private static final Pattern EMAIL_ADDR_SPEC = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

	/**
	 * ITU-T E.164: a leading {@code +} (international dialing prefix) followed by up to 15 digits.
	 */
	private static final Pattern E164_PHONE_NUMBER = Pattern.compile("^\\+[1-9][0-9]{1,14}$");

	private SsfSubjectIdentifiers() {
	}

	/**
	 * Thrown by {@link #validate(JsonElement)} when a subject identifier is structurally invalid.
	 * The message is suitable for surfacing in a condition error.
	 */
	public static class InvalidSubjectIdentifierException extends IllegalArgumentException {

		private static final long serialVersionUID = 1L;

		public InvalidSubjectIdentifierException(String message) {
			super(message);
		}
	}

	/**
	 * Returns the {@code format} member of a subject identifier, or {@code null} if the element
	 * is not an object or has no string {@code format}.
	 */
	public static String getFormat(JsonElement subjectIdentifier) {
		if (subjectIdentifier == null || !subjectIdentifier.isJsonObject()) {
			return null;
		}
		JsonElement format = subjectIdentifier.getAsJsonObject().get("format");
		if (format == null || !format.isJsonPrimitive() || !format.getAsJsonPrimitive().isString()) {
			return null;
		}
		return OIDFJSON.getString(format);
	}

	public static boolean isComplex(JsonElement subjectIdentifier) {
		return FORMAT_COMPLEX.equals(getFormat(subjectIdentifier));
	}

	/**
	 * Returns the Subject Members of a Complex Subject (every member except {@code format}) in
	 * declaration order. Returns an empty map if the element is not a complex subject.
	 */
	public static Map<String, JsonElement> getComplexSubjectMembers(JsonElement subjectIdentifier) {
		Map<String, JsonElement> members = new LinkedHashMap<>();
		if (!isComplex(subjectIdentifier)) {
			return members;
		}
		for (Map.Entry<String, JsonElement> entry : subjectIdentifier.getAsJsonObject().entrySet()) {
			if (!"format".equals(entry.getKey())) {
				members.put(entry.getKey(), entry.getValue());
			}
		}
		return members;
	}

	/**
	 * Validates the structure of a subject identifier (simple or complex).
	 *
	 * @throws InvalidSubjectIdentifierException if the identifier is malformed
	 */
	public static void validate(JsonElement subjectIdentifier) {
		validate(subjectIdentifier, "subject identifier", true);
	}

	private static void validate(JsonElement subjectIdentifier, String what, boolean allowComplexOrNested) {

		if (subjectIdentifier == null || !subjectIdentifier.isJsonObject()) {
			throw new InvalidSubjectIdentifierException(what + " must be a JSON object");
		}
		JsonObject subject = subjectIdentifier.getAsJsonObject();

		JsonElement formatEl = subject.get("format");
		if (formatEl == null) {
			throw new InvalidSubjectIdentifierException(what + " is missing the required 'format' member");
		}
		if (!isNonEmptyString(formatEl)) {
			throw new InvalidSubjectIdentifierException(what + " 'format' member must be a non-empty string");
		}
		String format = OIDFJSON.getString(formatEl);

		switch (format) {
			case FORMAT_COMPLEX -> {
				if (!allowComplexOrNested) {
					throw new InvalidSubjectIdentifierException(what + " must be a Simple Subject Member, but uses format 'complex' (nesting of Complex Subjects is not allowed, SSF 1.0 section 3.3)");
				}
				validateComplex(subject, what);
			}
			case FORMAT_ALIASES -> {
				if (!allowComplexOrNested) {
					throw new InvalidSubjectIdentifierException(what + " must not use format 'aliases' (nesting of aliases is not allowed, RFC 9493 section 3.8)");
				}
				validateAliases(subject, what);
			}
			case FORMAT_IP_ADDRESSES -> validateStringArrayMember(subject, "ip-addresses", what + " (format 'ip-addresses')");
			default -> {
				List<String> requiredMembers = REQUIRED_STRING_MEMBERS.get(format);
				if (requiredMembers == null) {
					// proprietary format, see SSF 1.0 section 3.4 — nothing more to check
					return;
				}
				for (String member : requiredMembers) {
					requireNonEmptyString(subject, member, what + " (format '" + format + "')");
				}
				validateFormatSpecificSyntax(subject, format, what);
			}
		}
	}

	private static void validateFormatSpecificSyntax(JsonObject subject, String format, String what) {
		switch (format) {
			case FORMAT_EMAIL -> {
				String email = OIDFJSON.getString(subject.get("email"));
				if (!EMAIL_ADDR_SPEC.matcher(email).matches()) {
					throw new InvalidSubjectIdentifierException(what + " 'email' member '" + email + "' is not a valid RFC 5322 addr-spec");
				}
			}
			case FORMAT_PHONE_NUMBER -> {
				String phoneNumber = OIDFJSON.getString(subject.get("phone_number"));
				if (!E164_PHONE_NUMBER.matcher(phoneNumber).matches()) {
					throw new InvalidSubjectIdentifierException(what + " 'phone_number' member '" + phoneNumber + "' is not in E.164 format with international dialing prefix (e.g. +12065550100)");
				}
			}
			case FORMAT_ACCOUNT -> {
				String uri = OIDFJSON.getString(subject.get("uri"));
				if (!uri.startsWith("acct:")) {
					throw new InvalidSubjectIdentifierException(what + " 'uri' member '" + uri + "' must be an 'acct' URI (RFC 7565)");
				}
			}
			case FORMAT_DID -> {
				String url = OIDFJSON.getString(subject.get("url"));
				if (!url.startsWith("did:")) {
					throw new InvalidSubjectIdentifierException(what + " 'url' member '" + url + "' must be a DID URL");
				}
			}
			default -> {
				// no additional syntax constraints
			}
		}
	}

	private static void validateComplex(JsonObject subject, String what) {
		Map<String, JsonElement> members = getComplexSubjectMembers(subject);
		if (members.isEmpty()) {
			throw new InvalidSubjectIdentifierException(what + " with format 'complex' must contain at least one Subject Member (SSF 1.0 section 3.3)");
		}
		for (Map.Entry<String, JsonElement> member : members.entrySet()) {
			validate(member.getValue(), what + " Subject Member '" + member.getKey() + "'", false);
		}
	}

	private static void validateAliases(JsonObject subject, String what) {
		JsonElement identifiersEl = subject.get("identifiers");
		if (identifiersEl == null || !identifiersEl.isJsonArray()) {
			throw new InvalidSubjectIdentifierException(what + " (format 'aliases') must contain an 'identifiers' array");
		}
		JsonArray identifiers = identifiersEl.getAsJsonArray();
		if (identifiers.isEmpty()) {
			throw new InvalidSubjectIdentifierException(what + " (format 'aliases') 'identifiers' array must contain at least one subject identifier");
		}
		for (int i = 0; i < identifiers.size(); i++) {
			validate(identifiers.get(i), what + " alias identifiers[" + i + "]", false);
		}
	}

	private static void validateStringArrayMember(JsonObject subject, String member, String what) {
		JsonElement el = subject.get(member);
		if (el == null || !el.isJsonArray()) {
			throw new InvalidSubjectIdentifierException(what + " must contain a '" + member + "' array");
		}
		JsonArray array = el.getAsJsonArray();
		if (array.isEmpty()) {
			throw new InvalidSubjectIdentifierException(what + " '" + member + "' array must not be empty");
		}
		for (JsonElement item : array) {
			if (!isNonEmptyString(item)) {
				throw new InvalidSubjectIdentifierException(what + " '" + member + "' array must only contain non-empty strings");
			}
		}
	}

	private static void requireNonEmptyString(JsonObject subject, String member, String what) {
		JsonElement el = subject.get(member);
		if (el == null) {
			throw new InvalidSubjectIdentifierException(what + " is missing the required '" + member + "' member");
		}
		if (!isNonEmptyString(el)) {
			throw new InvalidSubjectIdentifierException(what + " '" + member + "' member must be a non-empty string");
		}
	}

	private static boolean isNonEmptyString(JsonElement el) {
		return el != null
			&& el.isJsonPrimitive()
			&& el.getAsJsonPrimitive().isString()
			&& !OIDFJSON.getString(el).isEmpty();
	}
}
