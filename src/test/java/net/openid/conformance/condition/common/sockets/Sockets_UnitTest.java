package net.openid.conformance.condition.common.sockets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Sockets_UnitTest {

	@Test
	public void convertResponseForEnvironment_storesStatusNameHeadersAndBody() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json;charset=UTF-8");
		headers.add("Set-Cookie", "a=1");
		headers.add("Set-Cookie", "b=2");
		ResponseEntity<String> response = new ResponseEntity<>("{\"access_token\":\"x\"}", headers, HttpStatus.OK);

		JsonObject resp = Sockets.convertResponseForEnvironment("Call token endpoint", response);

		assertEquals(JsonParser.parseString("""
			{
				"status": 200,
				"endpoint_name": "Call token endpoint",
				"headers": {
					"content-type": "application/json;charset=UTF-8",
					"set-cookie": ["a=1", "b=2"]
				},
				"body": "{\\"access_token\\":\\"x\\"}",
				"body_json": {"access_token": "x"}
			}
			"""), resp);
	}

	@Test
	public void convertResponseForEnvironment_keepsErrorResponseBody() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		ResponseEntity<String> response = new ResponseEntity<>("{\"error\":\"invalid_grant\"}", headers, HttpStatus.BAD_REQUEST);

		JsonObject resp = Sockets.convertResponseForEnvironment("Call token endpoint", response);

		assertEquals(400, OIDFJSON.getInt(resp.get("status")));
		assertEquals(JsonParser.parseString("{\"error\":\"invalid_grant\"}"), resp.get("body_json"));
	}

	@Test
	public void convertResponseForEnvironment_doesNotParseNonJsonContentType() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/jwt");
		ResponseEntity<String> response = new ResponseEntity<>("{\"looks\":\"like json\"}", headers, HttpStatus.OK);

		JsonObject resp = Sockets.convertResponseForEnvironment("Retrieve SSA", response);

		assertFalse(resp.has("body_json"));
		assertEquals("{\"looks\":\"like json\"}", OIDFJSON.getString(resp.get("body")));
	}

	@Test
	public void convertResponseForEnvironment_handlesEmptyBody() {
		ResponseEntity<String> response = new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NO_CONTENT);

		JsonObject resp = Sockets.convertResponseForEnvironment("Unregister client1", response);

		assertEquals(204, OIDFJSON.getInt(resp.get("status")));
		assertFalse(resp.has("body_json"));
	}
}
