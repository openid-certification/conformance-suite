package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCICaptureCredentialForLinkability_UnitTest {

	// "Wed, 21 Oct 2015 07:28:00 GMT" in epoch seconds
	private static final String HTTP_DATE = "Wed, 21 Oct 2015 07:28:00 GMT";
	private static final long HTTP_DATE_EPOCH = 1445412480L;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCICaptureCredentialForLinkability cond;

	@BeforeEach
	public void setUp() {
		cond = new VCICaptureCredentialForLinkability();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void putDateHeader(String date) {
		JsonObject headers = new JsonObject();
		if (date != null) {
			headers.addProperty("date", date);
		}
		env.putObject("resource_endpoint_response_headers", headers);
	}

	private void putSdJwt() {
		JsonObject sdjwt = new JsonObject();
		sdjwt.addProperty("credential", "dummy");
		env.putObject("sdjwt", sdjwt);
	}

	private JsonArray captures() {
		JsonObject container = env.getObject("linkability_captures");
		return container == null ? null : container.getAsJsonArray("list");
	}

	@Test
	public void sdJwt_capturedWithParsedDateHeader() {
		putSdJwt();
		putDateHeader(HTTP_DATE);

		assertDoesNotThrow(() -> cond.execute(env));

		JsonArray list = captures();
		assertEquals(1, list.size());
		JsonObject entry = list.get(0).getAsJsonObject();
		assertEquals("sd_jwt_vc", OIDFJSON.getString(entry.get("format")));
		assertEquals(HTTP_DATE_EPOCH, OIDFJSON.getLong(entry.get("response_time")));
		assertFalse(OIDFJSON.getBoolean(entry.get("response_time_is_fallback")));
		assertTrue(entry.get("sdjwt").isJsonObject());
	}

	@Test
	public void mdoc_capturedWithRawCbor() {
		env.putString("mdoc_credential_cbor", "AAAA");
		putDateHeader(HTTP_DATE);

		assertDoesNotThrow(() -> cond.execute(env));

		JsonObject entry = captures().get(0).getAsJsonObject();
		assertEquals("mso_mdoc", OIDFJSON.getString(entry.get("format")));
		assertEquals("AAAA", OIDFJSON.getString(entry.get("mdoc_credential_cbor")));
	}

	@Test
	public void garbledDateHeader_usesFlaggedFallback() {
		putSdJwt();
		putDateHeader("not a date");

		long before = Instant.now().getEpochSecond();
		assertDoesNotThrow(() -> cond.execute(env));
		long after = Instant.now().getEpochSecond();

		JsonObject entry = captures().get(0).getAsJsonObject();
		assertTrue(OIDFJSON.getBoolean(entry.get("response_time_is_fallback")));
		long responseTime = OIDFJSON.getLong(entry.get("response_time"));
		assertTrue(responseTime >= before && responseTime <= after);
	}

	@Test
	public void missingDateHeader_usesFlaggedFallback() {
		putSdJwt();
		putDateHeader(null);

		assertDoesNotThrow(() -> cond.execute(env));

		JsonObject entry = captures().get(0).getAsJsonObject();
		assertTrue(OIDFJSON.getBoolean(entry.get("response_time_is_fallback")));
	}

	@Test
	public void secondExecution_appendsToList() {
		putSdJwt();
		putDateHeader(HTTP_DATE);

		assertDoesNotThrow(() -> cond.execute(env));
		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(2, captures().size());
	}

	@Test
	public void noCredentialPresent_capturesNothing() {
		putDateHeader(HTTP_DATE);

		assertDoesNotThrow(() -> cond.execute(env));

		assertNull(captures());
	}
}
