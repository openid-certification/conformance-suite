package net.openid.conformance.logging;

import com.google.common.base.Splitter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class JsonObjectSanitiserTests {

	@Test
	public void findLeavesOfJsonObject() throws IOException {

		JsonObject jsonObject = load("manyThings.json");
		JsonObject expected = load("manyThingsSanitised.json");

		JsonObjectSanitiser sanitiser = new JsonObjectSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor()));
		Set<JsonObjectSanitiser.LeafNode> leafNodes = sanitiser.findLeafNodes("test", jsonObject);

		assertEquals(7, leafNodes.size());

		sanitiser.sanitise(leafNodes);

		assertEquals(expected, jsonObject);
	}

	@Test
	public void nonKeysAreNotObfuscatedInAJsonObject() throws IOException {

		EventLog delegate = mock(EventLog.class);

		JsonObject jsonObject = load("notActuallyAKey.json");

		JsonObjectSanitiser walker = new JsonObjectSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor()));
		EventLog log = new SanitisingEventLog(delegate, walker, new MapSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor())));

		log.log("test", "source", null, jsonObject);

		ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
		Mockito.verify(delegate).log(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Map.class), captor.capture());

		JsonObject captured = captor.getValue();

		JsonElement found = findByPath(captured, "foo.bar.wibble.key");

		assertEquals("this isn't really a private key", OIDFJSON.getString(found));

	}

	@Test
	public void sanitisePrivateKeyInJsonObject() throws NoSuchAlgorithmException, IOException {

		JsonObject jsonObject = load("withPrivateKey.json");

		String pub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAglzC3Z/gVVcTWxJn7AVK02uKaQhn3GTc1CbVjm+cW/T/7L2A2WYNrtWFQFYne4Si+nMxdZ1NS1+UbI5NST0WcwrEHwuu1ALXekbarUewlrX41GdsdXk6uagdyB5jh7d7ibrhpWlIDv/m/fCcpDNrCXYtsCY8T4V46hBFjqtpcta9Cz9bv3SvAt/6X+15UgPmtP3itxhoWFSYXuP5tXDUjHUHI5Jt4zaLK+xs/NVIkjk3r4my8zbQPG/7KRSeQFEf7ggKd3NK3CvI0E8CRPNLCSuHYbDrT1dAxk/O4NFCA2P2s3MozVX2WJTr9AYpqHekKLu4uaWmJY73AKj81YN4mQIDAQAB";
		EventLog delegate = mock(EventLog.class);

		JsonObjectSanitiser walker = new JsonObjectSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor()));
		EventLog log = new SanitisingEventLog(delegate, walker, new MapSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor())));

		log.log("test", "source", null, jsonObject);

		ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
		Mockito.verify(delegate).log(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Map.class), captor.capture());

		JsonObject captured = captor.getValue();

		JsonElement found = findByPath(captured, "foo.bar.wibble.key");

		JsonObject value = found.getAsJsonObject();

		assertEquals(pub, OIDFJSON.getString(value.get("publicKey")));
		assertEquals("<obfuscated for security>", OIDFJSON.getString(value.get("privateKey")));

	}

	@Test
	public void sanitisePrivateKeyNotCalledKeyInJsonObject() throws NoSuchAlgorithmException, IOException {

		JsonObject jsonObject = load("withSneakyPrivateKey.json");

		String pub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAglzC3Z/gVVcTWxJn7AVK02uKaQhn3GTc1CbVjm+cW/T/7L2A2WYNrtWFQFYne4Si+nMxdZ1NS1+UbI5NST0WcwrEHwuu1ALXekbarUewlrX41GdsdXk6uagdyB5jh7d7ibrhpWlIDv/m/fCcpDNrCXYtsCY8T4V46hBFjqtpcta9Cz9bv3SvAt/6X+15UgPmtP3itxhoWFSYXuP5tXDUjHUHI5Jt4zaLK+xs/NVIkjk3r4my8zbQPG/7KRSeQFEf7ggKd3NK3CvI0E8CRPNLCSuHYbDrT1dAxk/O4NFCA2P2s3MozVX2WJTr9AYpqHekKLu4uaWmJY73AKj81YN4mQIDAQAB";
		EventLog delegate = mock(EventLog.class);

		JsonObjectSanitiser walker = new JsonObjectSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor()));
		EventLog log = new SanitisingEventLog(delegate, walker, new MapSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor())));

		log.log("test", "source", null, jsonObject);

		ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
		Mockito.verify(delegate).log(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Map.class), captor.capture());

		JsonObject captured = captor.getValue();

		JsonElement found = findByPath(captured, "foo.bar.wibble.bananas");

		JsonObject value = found.getAsJsonObject();

		assertEquals(pub, OIDFJSON.getString(value.get("publicKey")));
		assertEquals("<obfuscated for security>", OIDFJSON.getString(value.get("privateKey")));

	}

	@Test
	public void sanitiseJwksInObjecvt() throws IOException {

		EventLog delegate = mock(EventLog.class);

		JsonObject jsonObject = load("objectWithJwks.json");

		JsonObjectSanitiser walker = new JsonObjectSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor()));
		EventLog log = new SanitisingEventLog(delegate, walker, new MapSanitiser(Set.of(new JwksLeafNodeVisitor(), new PrivateKeyLeafVisitor())));

		log.log("test", "source", null, jsonObject);

		ArgumentCaptor<JsonObject> captor = ArgumentCaptor.forClass(JsonObject.class);
		Mockito.verify(delegate).log(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(Map.class), captor.capture());

		JsonObject captured = captor.getValue();

		JsonElement found = findByPath(captured, "jwks");

		JsonObject value = found.getAsJsonObject();
		assertEquals(1, value.size());

	}

	private JsonElement findByPath(JsonObject object, String path) {
		Iterable<String> parts = Splitter.on('.').split(path);
		Iterator<String> it = parts.iterator();

		return findByPath(object, path, it);
	}

	private JsonElement findByPath(JsonObject object, String path, Iterator<String> iterator) {
		String a = iterator.next();
		if(iterator.hasNext()) {
			return findByPath(object.get(a).getAsJsonObject(), path, iterator);
		}
		return object.get(a);
	}

	private JsonObject load(String name) throws IOException {
		String jsonString = IOUtils.resourceToString("jsonEnvironmentObjects/".concat(name), Charset.defaultCharset(), getClass().getClassLoader());
		return new JsonParser().parse(jsonString).getAsJsonObject();
	}

}
