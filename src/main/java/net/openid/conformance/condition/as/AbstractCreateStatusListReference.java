package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

/**
 * Allocates the Token Status List reference for a credential the test suite is about to create
 * and present, pointing at the status list this test instance serves.
 *
 * <p>Subclasses choose whether the index is one {@link EvenOddStatusListContents} marks VALID or
 * INVALID. It is picked at random rather than fixed so that two runs of the test do not present
 * credentials a verifier could correlate by index. The reference is stored in
 * {@code status_list_reference}; the conditions that create the credential turn it into the
 * format specific reference ({@code status.status_list} in an SD-JWT VC, the MSO's status
 * element in an mdoc) and the conditions that generate the status list token use the URI as
 * the token's subject. It also records whether it revokes the credential, which decides how
 * much it matters that the verifier looks the credential up.
 *
 * <p>The URI ends in a random path segment. A test instance's base url stays the same from one
 * run to the next when the test configuration sets an alias, and draft-ietf-oauth-status-list
 * section 13.7 recommends that a Relying Party caches a status list for its ttl; with a fixed
 * path a verifier could answer from a copy it fetched in an earlier run and never ask this one.
 */
public abstract class AbstractCreateStatusListReference extends AbstractCondition {

	/**
	 * Prefix of the path, relative to the test instance's base url, that the status list is
	 * served from; the random segment that follows it is allocated per test instance.
	 */
	public static final String STATUS_LIST_PATH_PREFIX = "statuslists/";

	public static final String ENV_KEY = "status_list_reference";

	/** Whether the allocated index is one the served status list marks as revoked. */
	protected abstract boolean revoked();

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {

		SecureRandom random = new SecureRandom();
		long idx = (revoked()
			? EvenOddStatusListContents.allocateRevokedIndices(1, random)
			: EvenOddStatusListContents.allocateValidIndices(1, random)).get(0);

		String path = STATUS_LIST_PATH_PREFIX + RandomStringUtils.secure().nextAlphanumeric(20);

		JsonObject reference = new JsonObject();
		reference.addProperty("path", path);
		reference.addProperty("uri", env.getString("base_url") + "/" + path);
		reference.addProperty("idx", idx);
		reference.addProperty("revoked", revoked());
		env.putObject(ENV_KEY, reference);

		logSuccess("Allocated a status list index that the served status list marks as "
				+ (revoked() ? "revoked" : "valid"),
			args("status_list_reference", reference));

		return env;
	}
}
