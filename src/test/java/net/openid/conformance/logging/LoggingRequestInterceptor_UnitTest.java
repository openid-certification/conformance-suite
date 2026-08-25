package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoggingRequestInterceptor_UnitTest {

	@Test
	public void textBodyIsLoggedVerbatim() {
		JsonObject o = new JsonObject();
		String body = "{\"json\": \"value\"}\r\n\ttext with whitespace controls";
		LoggingRequestInterceptor.addBodyProperty(o, "response_body", body.getBytes(StandardCharsets.UTF_8));

		assertEquals(body, OIDFJSON.getString(o.get("response_body")));
		assertFalse(o.has("response_body_omitted"));
	}

	@Test
	public void utf8MultibyteBodyIsLoggedVerbatim() {
		JsonObject o = new JsonObject();
		String body = "Ün été — 日本語";
		LoggingRequestInterceptor.addBodyProperty(o, "response_body", body.getBytes(StandardCharsets.UTF_8));

		assertEquals(body, OIDFJSON.getString(o.get("response_body")));
	}

	@Test
	public void binaryBodyIsOmittedWithNote() {
		JsonObject o = new JsonObject();
		// DER/CBOR-style bytes: invalid UTF-8
		byte[] body = new byte[] { (byte) 0xd2, (byte) 0x84, 0x43, (byte) 0xa1, 0x01, 0x26, (byte) 0xf6 };
		LoggingRequestInterceptor.addBodyProperty(o, "response_body", body);

		assertFalse(o.has("response_body"));
		String note = OIDFJSON.getString(o.get("response_body_omitted"));
		assertTrue(note.contains("7 bytes"), note);
		assertTrue(note.contains("not valid UTF-8: invalid byte sequence at byte offset 3"), note);
		// printable ASCII shown as-is, everything else hex-escaped
		assertEquals("\\xd2\\x84C\\xa1\\x01&\\xf6", OIDFJSON.getString(o.get("response_body_first_bytes")));
		// short body: the bad area is inside the first-bytes preview, no separate window
		assertFalse(o.has("response_body_bytes_around_offset"));
	}

	@Test
	public void binaryBodyPreviewShowsWindowAroundLaterBadOffset() {
		JsonObject o = new JsonObject();
		byte[] body = new byte[100];
		for (int i = 0; i < body.length; i++) {
			body[i] = 'a';
		}
		body[80] = (byte) 0xfe; // never valid in UTF-8
		LoggingRequestInterceptor.addBodyProperty(o, "response_body", body);

		String note = OIDFJSON.getString(o.get("response_body_omitted"));
		assertTrue(note.contains("invalid byte sequence at byte offset 80"), note);
		assertEquals("a".repeat(32), OIDFJSON.getString(o.get("response_body_first_bytes")));
		String around = OIDFJSON.getString(o.get("response_body_bytes_around_offset"));
		assertEquals("bytes 64..95: " + "a".repeat(16) + "\\xfe" + "a".repeat(15), around);
	}

	@Test
	public void validUtf8WithControlCharactersIsTreatedAsBinary() {
		JsonObject o = new JsonObject();
		// decodes as UTF-8 but contains a NUL - not text (the content-type header plays no part)
		byte[] body = "not\u0000text".getBytes(StandardCharsets.UTF_8);
		LoggingRequestInterceptor.addBodyProperty(o, "response_body", body);

		assertFalse(o.has("response_body"));
		String note = OIDFJSON.getString(o.get("response_body_omitted"));
		assertTrue(note.contains("contains control character U+0000 at character offset 3"), note);
	}
}
