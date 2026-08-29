package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;

/**
 * Allocates the Token Status List reference for a credential the test suite is about to create
 * and present, pointing at the status list this test instance serves and at an index that list
 * marks as revoked.
 *
 * <p>{@link EvenOddStatusListContents} marks every odd index INVALID and every even index VALID,
 * so a revoked credential gets a randomly chosen odd index. The index is stored alongside the
 * URI in {@code status_list_reference}; the conditions that create the credential turn it into
 * the format specific reference ({@code status.status_list} in an SD-JWT VC, the MSO's status
 * element in an mdoc) and the conditions that generate the status list token use the URI as the
 * token's subject.
 */
public class CreateRevokedStatusListReference extends AbstractCondition {

	/** Path, relative to the test instance's base url, that the status list is served from. */
	public static final String STATUS_LIST_PATH = "statuslists/1";

	public static final String ENV_KEY = "status_list_reference";

	private final SecureRandom random = new SecureRandom();

	@Override
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		if (baseUrl == null) {
			throw error("The test instance has no base url, so the status list the credential"
				+ " references cannot be published");
		}

		// odd indices are the revoked ones; chosen at random rather than fixed so that two runs
		// of the test do not present credentials that a verifier could correlate by index
		int idx = 1 + 2 * random.nextInt(EvenOddStatusListContents.STATUS_LIST_ENTRIES / 2);
		String uri = baseUrl + "/" + STATUS_LIST_PATH;

		JsonObject reference = new JsonObject();
		reference.addProperty("uri", uri);
		reference.addProperty("idx", idx);
		env.putObject(ENV_KEY, reference);

		logSuccess("Allocated a status list index that the served status list marks as revoked",
			args("uri", uri, "idx", idx));

		return env;
	}
}
