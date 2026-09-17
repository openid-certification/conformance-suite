package net.openid.conformance.statistics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerIds_UnitTest {

	@Test
	void theSameUserAlwaysGetsTheSameId() {
		OwnerIds ids = new OwnerIds();

		int first = ids.idFor("https://accounts.google.com", "1234");

		assertThat(ids.idFor("https://accounts.google.com", "1234")).isEqualTo(first);
	}

	@Test
	void differentUsersGetDifferentIds() {
		OwnerIds ids = new OwnerIds();

		assertThat(ids.idFor("https://accounts.google.com", "1234"))
			.isNotEqualTo(ids.idFor("https://accounts.google.com", "5678"));
		assertThat(ids.idFor("https://gitlab.com", "1234"))
			.isNotEqualTo(ids.idFor("https://accounts.google.com", "1234"));
	}

	@Test
	void anIssuerEndingWhereTheSubjectBeginsIsStillADifferentUser() {
		OwnerIds ids = new OwnerIds();

		assertThat(ids.idFor("ab", "cd")).isNotEqualTo(ids.idFor("a", "bcd"));
	}

	@Test
	void noPairOfIdentifiersCanCollideWhateverCharactersTheyContain() {
		OwnerIds ids = new OwnerIds();

		// each pair collides if the two identifiers are joined by that character
		assertThat(ids.idFor("a:b", "c")).isNotEqualTo(ids.idFor("a", "b:c"));
		assertThat(ids.idFor("a b", "c")).isNotEqualTo(ids.idFor("a", "b c"));
		assertThat(ids.idFor("a|b", "c")).isNotEqualTo(ids.idFor("a", "b|c"));
		assertThat(ids.idFor("a\u0000b", "c")).isNotEqualTo(ids.idFor("a", "b\u0000c"));
	}

	@Test
	void aRunWithNoIdentityIsNotAUser() {
		assertThat(OwnerIds.isUser(null, "1234")).isFalse();
		assertThat(OwnerIds.isUser("https://gitlab.com", null)).isFalse();
		assertThat(OwnerIds.isUser("", "1234")).isFalse();
		assertThat(OwnerIds.isUser("https://gitlab.com", "  ")).isFalse();
		assertThat(OwnerIds.isUser("https://gitlab.com", "1234")).isTrue();
	}
}
