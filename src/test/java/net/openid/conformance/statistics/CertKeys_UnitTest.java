package net.openid.conformance.statistics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CertKeys_UnitTest {

	@Test
	void oneProfileIsItsOwnName() {
		assertThat(CertKeys.canonical(List.of("FAPI2SP OP MTLS"))).isEqualTo("FAPI2SP OP MTLS");
	}

	@Test
	void severalProfilesAreJoinedWithTheSeparatorInTheStoredOrder() {
		assertThat(CertKeys.canonical(List.of("BR-OF Adv. OP", "AU-CDR Adv. OP", "3rd Party-Init OP")))
			.isEqualTo("BR-OF Adv. OP" + CertKeys.SEPARATOR + "AU-CDR Adv. OP" + CertKeys.SEPARATOR + "3rd Party-Init OP");
	}

	@Test
	void aPlanWithNoCertificationProfileHasTheEmptyKey() {
		assertThat(CertKeys.canonical(null)).isEmpty();
		assertThat(CertKeys.canonical(List.of())).isEmpty();
	}

	@Test
	void aSingleStringIsAcceptedAsWellAsAList() {
		assertThat(CertKeys.canonical("FAPI2SP OP MTLS")).isEqualTo("FAPI2SP OP MTLS");
		assertThat(CertKeys.canonical("  ")).isEmpty();
	}

	@Test
	void nullAndBlankEntriesAreDroppedAndTheRestAreTrimmed() {
		List<String> profiles = new ArrayList<>();
		profiles.add(null);
		profiles.add("  FAPI2SP OP MTLS  ");
		profiles.add("");

		assertThat(CertKeys.canonical(profiles)).isEqualTo("FAPI2SP OP MTLS");
	}

	@Test
	void aSeparatorInsideAProfileNameCannotSplitTheKeyIntoMorePartsThanItWasBuiltFrom() {
		String key = CertKeys.canonical(List.of("one" + CertKeys.SEPARATOR + "two", "three"));

		assertThat(key.split(java.util.regex.Pattern.quote(CertKeys.SEPARATOR))).hasSize(2);
	}

	@Test
	void anythingThatIsNotAProfileNameIsTheEmptyKey() {
		assertThat(CertKeys.canonical(42)).isEmpty();
		assertThat(CertKeys.canonical(List.of(1, 2))).isEmpty();
	}
}
