package net.openid.conformance.info;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reading {@code owner=<sub>&owner_iss=<iss>}, which is what scopes a listing - and the bulk
 * delete - to one account.
 */
public class PlanOwner_UnitTest {

	private static Map<String, String[]> params(String... pairs) {
		Map<String, String[]> params = new java.util.LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			params.put(pairs[i], new String[] { pairs[i + 1] });
		}
		return params;
	}

	@Test
	void noOwnerAtAllIsNotNarrowingToOne() {
		assertThat(PlanOwner.parse(params())).isNull();
		assertThat(PlanOwner.parse(params("plan", "a-plan"))).isNull();
	}

	@Test
	void bothHalvesNameAnAccount() {
		PlanOwner owner = PlanOwner.parse(params("owner", "12345", "owner_iss", "https://gitlab.com"));

		assertThat(owner).isEqualTo(new PlanOwner("12345", "https://gitlab.com"));
	}

	@Test
	void aSubWithoutItsIssuerIsRefused() {
		// the whole point: 'sub' 12345 is a gitlab.com account AND, in principle, a google one,
		// and this scope drives the bulk delete
		assertThatThrownBy(() -> PlanOwner.parse(params("owner", "12345")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("owner_iss");
	}

	@Test
	void anIssuerWithoutASubIsRefusedToo() {
		// otherwise it would silently list every account of that issuer
		assertThatThrownBy(() -> PlanOwner.parse(params("owner_iss", "https://gitlab.com")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("owner");
	}

	@Test
	void aBlankHalfCountsAsMissingRatherThanAsAValue() {
		// QueryParams treats a blank parameter as absent, so ?owner=&owner_iss= is "no owner"
		// and ?owner=12345&owner_iss= is a half pair rather than a match on an empty issuer -
		// there are 80 plans in production whose owner.sub is the empty string
		assertThat(PlanOwner.parse(params("owner", "  ", "owner_iss", ""))).isNull();
		assertThatThrownBy(() -> PlanOwner.parse(params("owner", "12345", "owner_iss", " ")))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void bothHalvesAreMatched() {
		Document criteria = new PlanOwner("12345", "https://gitlab.com").toCriteria().getCriteriaObject();

		assertThat(criteria).isEqualTo(new Document("owner.sub", "12345").append("owner.iss", "https://gitlab.com"));
	}
}
