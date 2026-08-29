package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;

/**
 * Allocates the Token Status List reference for a credential the test suite is about to create
 * and present, pointing at the status list this test instance serves.
 *
 * <p>Subclasses choose whether the index is one {@link EvenOddStatusListContents} marks VALID or
 * INVALID. It is picked at random rather than fixed so that two runs of the test do not present
 * credentials a verifier could correlate by index. The reference is stored as a
 * {@link RevocationListReference}; the conditions that create the credential turn it into the
 * format specific reference ({@code status.status_list} in an SD-JWT VC, the MSO's status
 * element in an mdoc) and the conditions that generate the status list token use the URI as
 * the token's subject.
 */
public abstract class AbstractCreateStatusListReference extends AbstractCondition {

	/** Whether the allocated index is one the served status list marks as revoked. */
	protected abstract boolean revoked();

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = RevocationListReference.ENV_KEY)
	public Environment evaluate(Environment env) {

		SecureRandom random = new SecureRandom();
		long idx = (revoked()
			? EvenOddStatusListContents.allocateRevokedIndices(1, random)
			: EvenOddStatusListContents.allocateValidIndices(1, random)).get(0);

		JsonObject reference = RevocationListReference.allocate(env,
			RevocationListReference.MECHANISM_STATUS_LIST, RevocationListReference.STATUS_LIST_PATH_PREFIX);
		reference.addProperty("idx", idx);
		env.putObject(RevocationListReference.ENV_KEY, reference);

		logSuccess("Allocated a status list index that the served status list marks as "
				+ (revoked() ? "revoked" : "valid"),
			args("revocation_list_reference", reference));

		return env;
	}
}
