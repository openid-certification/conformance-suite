package net.openid.conformance.logging;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static net.openid.conformance.logging.MapCopy.deepCopy;
import static org.junit.Assert.fail;

public class MapCopyTests {

	@Test
	public void deepCopyMap() throws NoSuchAlgorithmException {

		Map<String, Object> messageMap = Map.of(
			"something", "Not relevant",
			"foo", Map.of("bar", "wibble")
		);

		Map<String, Object> inner = (Map<String, Object>) messageMap.get("foo");

		try {
			inner.put("baz", "cludge");
			fail();
		} catch (UnsupportedOperationException e) {}

		Map<String, Object> copy = deepCopy(messageMap);

		copy.put("new", "value");
		inner = (Map<String, Object>) copy.get("foo");
		inner.put("baz", "cludge");
	}

}
