package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DcqlQueryUtils_UnitTest {

	@Test
	void findCredentialById_matchesById() {
		JsonObject dcql = createDcqlWithCredential("cred1");
		JsonObject result = DcqlQueryUtils.findCredentialById(dcql, "cred1");
		assertNotNull(result);
	}

	@Test
	void findCredentialById_returnsNullForNoMatch() {
		JsonObject dcql = createDcqlWithCredential("cred1");
		assertNull(DcqlQueryUtils.findCredentialById(dcql, "cred2"));
	}

	@Test
	void findCredentialById_returnsNullForNullCredentials() {
		JsonObject dcql = new JsonObject();
		assertNull(DcqlQueryUtils.findCredentialById(dcql, "cred1"));
	}

	@Test
	void findCredentialById_matchesAmongMultiple() {
		JsonObject dcql = new JsonObject();
		JsonArray credentials = new JsonArray();
		credentials.add(createCredentialEntry("cred1"));
		credentials.add(createCredentialEntry("cred2"));
		dcql.add("credentials", credentials);

		JsonObject result = DcqlQueryUtils.findCredentialById(dcql, "cred2");
		assertNotNull(result);
		assertEquals("cred2", OIDFJSON.getString(result.get("id")));
	}

	@Test
	void findCredentialById_skipsCredentialWithoutId() {
		JsonObject dcql = new JsonObject();
		JsonArray credentials = new JsonArray();
		JsonObject noId = new JsonObject();
		noId.addProperty("format", "dc+sd-jwt");
		credentials.add(noId);
		credentials.add(createCredentialEntry("cred1"));
		dcql.add("credentials", credentials);

		assertNotNull(DcqlQueryUtils.findCredentialById(dcql, "cred1"));
		assertNull(DcqlQueryUtils.findCredentialById(dcql, "no-match"));
	}

	@Test
	void extractRequestedClaimNames_extractsTopLevelPaths() {
		JsonObject dcql = createDcqlWithClaims("given_name", "family_name");
		Set<String> claims = DcqlQueryUtils.extractRequestedClaimNames(dcql);
		assertEquals(Set.of("given_name", "family_name"), claims);
	}

	@Test
	void extractRequestedClaimNames_returnsEmptyForNoClaims() {
		JsonObject dcql = createDcqlWithCredential("cred1");
		Set<String> claims = DcqlQueryUtils.extractRequestedClaimNames(dcql);
		assertTrue(claims.isEmpty());
	}

	@Test
	void extractRequestedClaimNames_returnsEmptyForNullCredentials() {
		JsonObject dcql = new JsonObject();
		Set<String> claims = DcqlQueryUtils.extractRequestedClaimNames(dcql);
		assertTrue(claims.isEmpty());
	}

	@Test
	void extractClaimNamesFromCredential_extractsPaths() {
		JsonObject cred = createCredentialEntry("cred1");
		JsonArray claims = new JsonArray();
		claims.add(createClaim("given_name"));
		claims.add(createClaim("birthdate"));
		cred.add("claims", claims);

		Set<String> result = DcqlQueryUtils.extractClaimNamesFromCredential(cred);
		assertEquals(Set.of("given_name", "birthdate"), result);
	}

	@Test
	void extractClaimNamesFromCredential_returnsEmptyForNoClaims() {
		JsonObject cred = createCredentialEntry("cred1");
		assertTrue(DcqlQueryUtils.extractClaimNamesFromCredential(cred).isEmpty());
	}

	private JsonObject createDcqlWithCredential(String id) {
		JsonObject dcql = new JsonObject();
		JsonArray credentials = new JsonArray();
		credentials.add(createCredentialEntry(id));
		dcql.add("credentials", credentials);
		return dcql;
	}

	private JsonObject createDcqlWithClaims(String... claimNames) {
		JsonObject cred = createCredentialEntry("my_credential");
		JsonArray claims = new JsonArray();
		for (String name : claimNames) {
			claims.add(createClaim(name));
		}
		cred.add("claims", claims);

		JsonObject dcql = new JsonObject();
		JsonArray credentials = new JsonArray();
		credentials.add(cred);
		dcql.add("credentials", credentials);
		return dcql;
	}

	private JsonObject createCredentialEntry(String id) {
		JsonObject cred = new JsonObject();
		cred.addProperty("id", id);
		cred.addProperty("format", "dc+sd-jwt");
		return cred;
	}

	private JsonObject createClaim(String name) {
		JsonObject claim = new JsonObject();
		JsonArray path = new JsonArray();
		path.add(name);
		claim.add("path", path);
		return claim;
	}
}
