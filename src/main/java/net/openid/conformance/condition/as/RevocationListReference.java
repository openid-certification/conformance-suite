package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * The reference to a revocation list that a credential the test suite is about to create and
 * present will carry, pointing at the list this test instance serves. Stored in
 * {@code revocation_list_reference} by the condition that allocates it; a test presenting a
 * credential without revocation information allocates none.
 *
 * <p>Every reference has the {@code mechanism} it uses, the {@code uri} the list is published at
 * and that URI's {@code path} relative to the test instance's base url, plus what identifies the
 * credential on the list: {@code idx} for a status list.
 *
 * <p>The URI ends in a random path segment. A test instance's base url stays the same from one
 * run to the next when the test configuration sets an alias, and draft-ietf-oauth-status-list
 * section 13.7 recommends that a Relying Party caches a status list for its ttl; with a fixed
 * path a verifier could answer from a copy it fetched in an earlier run and never ask this one.
 */
public final class RevocationListReference {

	public static final String ENV_KEY = "revocation_list_reference";

	/** The Token Status List mechanism of draft-ietf-oauth-status-list. */
	public static final String MECHANISM_STATUS_LIST = "status_list";

	/**
	 * Prefix of the path, relative to the test instance's base url, that a status list is served
	 * from; the random segment that follows it is allocated per test instance.
	 */
	public static final String STATUS_LIST_PATH_PREFIX = "statuslists/";

	private RevocationListReference() {
	}

	/** Whether a request to the given path is one for a revocation list. */
	public static boolean isRevocationListPath(String path) {
		return path.startsWith(STATUS_LIST_PATH_PREFIX);
	}

	/**
	 * Starts a reference of the given mechanism at a fresh URI under the given path prefix, for
	 * the caller to add the credential's place on the list to and store.
	 */
	static JsonObject allocate(Environment env, String mechanism, String pathPrefix) {
		String path = pathPrefix + RandomStringUtils.secure().nextAlphanumeric(20);

		JsonObject reference = new JsonObject();
		reference.addProperty("mechanism", mechanism);
		reference.addProperty("path", path);
		reference.addProperty("uri", env.getString("base_url") + "/" + path);
		return reference;
	}
}
