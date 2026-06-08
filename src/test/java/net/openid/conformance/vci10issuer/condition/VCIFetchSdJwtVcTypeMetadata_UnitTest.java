package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the SD-JWT VC Type Metadata fetch condition. The HTTP call is
 * faked by overriding {@code createRestTemplate}; the tests exercise the
 * condition's own validation of the response (status, empty/non-JSON/non-object
 * bodies) and its transport-error handling.
 */
@ExtendWith(MockitoExtension.class)
public class VCIFetchSdJwtVcTypeMetadata_UnitTest {

	private static final String URL = "https://issuer.example.com/credentials/pid";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	/** Returns a fixed (mocked) RestTemplate so the real HTTP stack is never touched. */
	private static final class TestableFetch extends VCIFetchSdJwtVcTypeMetadata {
		private final RestTemplate restTemplate;

		private TestableFetch(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		public RestTemplate createRestTemplate(Environment environment) {
			return restTemplate;
		}
	}

	private void putUrl(String url) {
		JsonObject vci = new JsonObject();
		if (url != null) {
			vci.addProperty("sdjwt_vc_type_metadata_url", url);
		}
		env.putObject("vci", vci);
	}

	/** A condition whose HTTP GET returns the given canned response. */
	private VCIFetchSdJwtVcTypeMetadata condReturning(ResponseEntity<String> response) {
		RestTemplate restTemplate = mock(RestTemplate.class);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
			.thenReturn(response);
		VCIFetchSdJwtVcTypeMetadata cond = new TestableFetch(restTemplate);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		return cond;
	}

	/** A condition whose HTTP GET throws a transport-level error. */
	private VCIFetchSdJwtVcTypeMetadata condThrowing(RuntimeException toThrow) {
		RestTemplate restTemplate = mock(RestTemplate.class);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
			.thenThrow(toThrow);
		VCIFetchSdJwtVcTypeMetadata cond = new TestableFetch(restTemplate);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		return cond;
	}

	@Test
	public void okJsonObject_storesParsedMetadataAndResponse() {
		String body = "{\"vct\":\"https://example.com/pid\",\"name\":\"PID\"}";
		VCIFetchSdJwtVcTypeMetadata cond = condReturning(new ResponseEntity<>(body, HttpStatus.OK));
		putUrl(URL);

		cond.execute(env);

		assertEquals("https://example.com/pid", env.getString("vci", "sdjwt_vc_type_metadata.vct"));
		assertEquals(body, env.getString("vci", "sdjwt_vc_type_metadata_endpoint_response.body"));
		assertTrue(env.getElementFromObject("vci", "sdjwt_vc_type_metadata").isJsonObject());
	}

	@Test
	public void okJsonArray_failsAsNotAnObject() {
		VCIFetchSdJwtVcTypeMetadata cond = condReturning(new ResponseEntity<>("[\"not\",\"an\",\"object\"]", HttpStatus.OK));
		putUrl(URL);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not a JSON object"));
		// The malformed document must not be stored as if it were valid metadata.
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata"));
	}

	@Test
	public void okHtmlErrorPage_failsAsNotValidJson() {
		// An issuer serving an HTML error page where JSON is expected: must fail the
		// JSON-parse step rather than blow up unexpectedly.
		VCIFetchSdJwtVcTypeMetadata cond = condReturning(new ResponseEntity<>("<html><body>error</body></html>", HttpStatus.OK));
		putUrl(URL);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not valid JSON"));
	}

	@Test
	public void okEmptyBody_fails() {
		VCIFetchSdJwtVcTypeMetadata cond = condReturning(new ResponseEntity<>("", HttpStatus.OK));
		putUrl(URL);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("empty"));
	}

	@Test
	public void non200Status_failsButStillRecordsResponse() {
		VCIFetchSdJwtVcTypeMetadata cond = condReturning(new ResponseEntity<>("not found", HttpStatus.NOT_FOUND));
		putUrl(URL);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("non-200"));
		// The response is captured for logging before the status check rejects it...
		assertEquals("not found", env.getString("vci", "sdjwt_vc_type_metadata_endpoint_response.body"));
		// ...but no parsed metadata is stored.
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata"));
	}

	@Test
	public void transportError_failsWithFetchMessage() {
		// e.g. a TLS handshake failure or connection refused surfaces as a
		// RestClientException; the condition reports it as a fetch failure.
		VCIFetchSdJwtVcTypeMetadata cond = condThrowing(new ResourceAccessException("Connection refused"));
		putUrl(URL);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Unable to fetch"));
		assertNull(env.getElementFromObject("vci", "sdjwt_vc_type_metadata"));
	}

	@Test
	public void missingUrl_fails() {
		// vci exists (satisfying @PreEnvironment) but the URL the caller should have
		// gated on is absent.
		VCIFetchSdJwtVcTypeMetadata cond = new VCIFetchSdJwtVcTypeMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		putUrl(null);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not set"));
	}
}
