package net.openid.conformance.util.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DpopNonceResponseHeader_UnitTest {

	private static JsonObject headers(String... nonceValues) {
		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json");
		if (nonceValues.length == 1) {
			headers.addProperty(DpopNonceResponseHeader.HEADER_NAME, nonceValues[0]);
		} else if (nonceValues.length > 1) {
			JsonArray values = new JsonArray();
			for (String value : nonceValues) {
				values.add(value);
			}
			headers.add(DpopNonceResponseHeader.HEADER_NAME, values);
		}
		return headers;
	}

	@Test
	public void testNoHeaderIsNeitherNonceNorViolation() {
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers());

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).isNull();
	}

	@Test
	public void testNullHeadersAreTreatedAsAbsent() {
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(null);

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).isNull();
	}

	@Test
	public void testValidNonceIsReturned() {
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers("eyJ7S_zG.eyJH0-HZ.HX4w-7v"));

		assertThat(result.nonce()).isEqualTo("eyJ7S_zG.eyJH0-HZ.HX4w-7v");
		assertThat(result.violation()).isNull();
	}

	@Test
	public void testRepeatedHeaderIsAViolation() {
		// RFC9449 section 8: there MUST NOT be more than one DPoP-Nonce header
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers("first-nonce", "second-nonce"));

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).contains("2 DPoP-Nonce headers");
	}

	@Test
	public void testEmptyNonceIsAViolation() {
		// RFC9449 section 8.1 defines the nonce as 1*NQCHAR, so it must not be empty
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers(""));

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).contains("empty");
	}

	@Test
	public void testSpaceIsNotAnAllowedNonceCharacter() {
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers("has a space"));

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).contains("1*NQCHAR");
	}

	@Test
	public void testDoubleQuoteAndBackslashAreNotAllowedNonceCharacters() {
		assertThat(DpopNonceResponseHeader.from(headers("has\"quote")).violation()).contains("1*NQCHAR");
		assertThat(DpopNonceResponseHeader.from(headers("has\\backslash")).violation()).contains("1*NQCHAR");
	}

	@Test
	public void testNonAsciiIsNotAnAllowedNonceCharacter() {
		DpopNonceResponseHeader result = DpopNonceResponseHeader.from(headers("nönce"));

		assertThat(result.nonce()).isNull();
		assertThat(result.violation()).contains("1*NQCHAR");
	}
}
