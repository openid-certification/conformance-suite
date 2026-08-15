package net.openid.conformance.statistics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Granularity_UnitTest {

	@Test
	void ofAcceptsTheTwoWireNamesAndNothingElse() {
		assertThat(Granularity.of("month")).isEqualTo(Granularity.MONTH);
		assertThat(Granularity.of("week")).isEqualTo(Granularity.WEEK);
		assertThat(Granularity.MONTH.key()).isEqualTo("month");
		assertThat(Granularity.WEEK.key()).isEqualTo("week");

		assertThatThrownBy(() -> Granularity.of("day"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("granularity")
			.hasMessageContaining("day");
	}

	@Test
	void periodOfIsTheMonthOrTheMondayOfTheIsoWeek() {
		LocalDate sunday = LocalDate.of(2026, 3, 15);
		LocalDate monday = LocalDate.of(2026, 3, 9);

		assertThat(Granularity.MONTH.periodOf(sunday)).isEqualTo("2026-03");
		assertThat(Granularity.WEEK.periodOf(sunday)).isEqualTo("2026-03-09");
		assertThat(Granularity.WEEK.periodOf(monday)).isEqualTo("2026-03-09");
		// the ISO week of 1 January 2026 (a Thursday) starts in December
		assertThat(Granularity.WEEK.periodOf(LocalDate.of(2026, 1, 1))).isEqualTo("2025-12-29");
	}

	@Test
	void nextStepsOnePeriod() {
		assertThat(Granularity.MONTH.next("2026-12")).isEqualTo("2027-01");
		assertThat(Granularity.WEEK.next("2026-12-28")).isEqualTo("2027-01-04");
	}

	@Test
	void normalisePeriodChecksTheFormatAndSnapsAWeekToItsMonday() {
		assertThat(Granularity.MONTH.normalisePeriod("from", "2026-03")).isEqualTo("2026-03");
		assertThat(Granularity.WEEK.normalisePeriod("from", "2026-03-15")).isEqualTo("2026-03-09");

		assertThatThrownBy(() -> Granularity.MONTH.normalisePeriod("from", "2026-3"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("from")
			.hasMessageContaining("YYYY-MM");
		assertThatThrownBy(() -> Granularity.MONTH.normalisePeriod("to", "2026-13"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("to");
		assertThatThrownBy(() -> Granularity.WEEK.normalisePeriod("from", "2026-03"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("YYYY-MM-DD");
		assertThatThrownBy(() -> Granularity.WEEK.normalisePeriod("from", "2026-02-30"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void isPeriodRecognisesOnlyItsOwnKeyFormat() {
		assertThat(Granularity.MONTH.isPeriod("2026-03")).isTrue();
		assertThat(Granularity.MONTH.isPeriod("2026-03-09")).isFalse();
		assertThat(Granularity.MONTH.isPeriod("2026-13")).isFalse();
		assertThat(Granularity.MONTH.isPeriod(null)).isFalse();
		assertThat(Granularity.WEEK.isPeriod("2026-03-09")).isTrue();
		assertThat(Granularity.WEEK.isPeriod("2026-03-10")).isFalse(); // not a Monday
		assertThat(Granularity.WEEK.isPeriod("")).isFalse();
	}
}
