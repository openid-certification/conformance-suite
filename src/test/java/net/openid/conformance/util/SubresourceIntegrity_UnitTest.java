package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubresourceIntegrity_UnitTest {

	private static final byte[] ABC = "abc".getBytes(StandardCharsets.UTF_8);
	private static final String SHA256_ABC_BASE64 = sha256Base64(ABC);

	private static String sha256Base64(byte[] body) {
		try {
			return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void verify_validSha256_returnsTrue() {
		assertTrue(SubresourceIntegrity.verify(ABC, "sha256-" + SHA256_ABC_BASE64));
	}

	@Test
	public void verify_validSha384_returnsTrue() throws NoSuchAlgorithmException {
		String b64 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-384").digest(ABC));
		assertTrue(SubresourceIntegrity.verify(ABC, "sha384-" + b64));
	}

	@Test
	public void verify_validSha512_returnsTrue() throws NoSuchAlgorithmException {
		String b64 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(ABC));
		assertTrue(SubresourceIntegrity.verify(ABC, "sha512-" + b64));
	}

	@Test
	public void verify_hashMismatch_returnsFalse() {
		assertFalse(SubresourceIntegrity.verify("different".getBytes(StandardCharsets.UTF_8),
			"sha256-" + SHA256_ABC_BASE64));
	}

	@Test
	public void verify_emptyMetadata_returnsTrue() {
		// W3C SRI §3.3.4 step 2: empty metadata vacuously matches
		assertTrue(SubresourceIntegrity.verify(ABC, ""));
		assertTrue(SubresourceIntegrity.verify(ABC, "   "));
	}

	@Test
	public void verify_strongestAlgorithmSelected_strongerMatches() throws NoSuchAlgorithmException {
		// sha256 entry is wrong, sha512 entry is right: §3.3.3 picks the
		// strongest tier (sha512), and that one matches → overall true.
		String sha512 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(ABC));
		String wrongSha256 = Base64.getEncoder().encodeToString(new byte[32]);
		assertTrue(SubresourceIntegrity.verify(ABC,
			"sha256-" + wrongSha256 + " sha512-" + sha512));
	}

	@Test
	public void verify_strongestAlgorithmSelected_weakerMatchIgnored() throws NoSuchAlgorithmException {
		// sha256 entry matches, sha512 entry doesn't: §3.3.3 picks sha512 tier
		// only; the sha256 match is ignored → overall false.
		String wrongSha512 = Base64.getEncoder().encodeToString(new byte[64]);
		assertFalse(SubresourceIntegrity.verify(ABC,
			"sha256-" + SHA256_ABC_BASE64 + " sha512-" + wrongSha512));
	}

	@Test
	public void parse_missingDash_throws() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> SubresourceIntegrity.parse("sha256NoDash"));
		assertTrue(e.getMessage().contains("missing algorithm separator"));
	}

	@Test
	public void parse_unsupportedAlgorithm_throws() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> SubresourceIntegrity.parse("md5-abc"));
		assertTrue(e.getMessage().contains("unsupported algorithm"));
	}

	@Test
	public void parse_emptyHash_throws() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> SubresourceIntegrity.parse("sha256-"));
		assertTrue(e.getMessage().contains("empty hash value"));
	}

	@Test
	public void parse_invalidBase64_throws() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> SubresourceIntegrity.parse("sha256-!!!!!!"));
		assertTrue(e.getMessage().contains("invalid base64"));
	}

	@Test
	public void parse_acceptsBase64UrlCharacters() {
		// The SD-JWT VC example for vct#integrity uses '-' and '_' (base64url),
		// even though W3C SRI nominally specifies standard base64.
		String urlHash = SHA256_ABC_BASE64.replace('+', '-').replace('/', '_').replace("=", "");
		assertTrue(SubresourceIntegrity.verify(ABC, "sha256-" + urlHash));
	}

	@Test
	public void parse_acceptsUnpaddedBase64() {
		String unpadded = SHA256_ABC_BASE64.replace("=", "");
		assertTrue(SubresourceIntegrity.verify(ABC, "sha256-" + unpadded));
	}

	@Test
	public void parse_acceptsOptionsSuffix() {
		// W3C SRI §3.5: "?option-expression" after a hash is reserved and ignored
		assertTrue(SubresourceIntegrity.verify(ABC, "sha256-" + SHA256_ABC_BASE64 + "?foo=bar"));
	}

	@Test
	public void parse_acceptsMultipleEntriesWhitespaceSeparated() {
		List<SubresourceIntegrity.Entry> entries = SubresourceIntegrity.parse(
			"sha256-" + SHA256_ABC_BASE64 + "  sha256-" + SHA256_ABC_BASE64);
		assertEquals(2, entries.size());
	}

	@Test
	public void parse_emptyString_returnsEmpty() {
		assertEquals(0, SubresourceIntegrity.parse("").size());
		assertEquals(0, SubresourceIntegrity.parse("   ").size());
	}

	@Test
	public void parse_null_throws() {
		assertThrows(IllegalArgumentException.class, () -> SubresourceIntegrity.parse(null));
	}

	@Test
	public void strongest_picksSha512OverSha256() throws NoSuchAlgorithmException {
		String sha512 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(ABC));
		List<SubresourceIntegrity.Entry> entries = SubresourceIntegrity.parse(
			"sha256-" + SHA256_ABC_BASE64 + " sha512-" + sha512);
		List<SubresourceIntegrity.Entry> strongest = SubresourceIntegrity.strongest(entries);
		assertEquals(1, strongest.size());
		assertEquals(SubresourceIntegrity.Algorithm.SHA512, strongest.get(0).algorithm());
	}
}
