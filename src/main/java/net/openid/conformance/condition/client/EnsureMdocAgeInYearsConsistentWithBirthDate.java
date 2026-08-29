package net.openid.conformance.condition.client;

import org.multipaz.cbor.CborInt;
import org.multipaz.cbor.DataItem;

import java.time.LocalDate;
import java.util.Map;

/**
 * Checks that age_in_years and age_birth_year agree with the mdoc's birth_date. Unlike
 * age_over_NN, neither ISO/IEC 18013-5 nor ISO/IEC TS 23220-2 states explicitly when these are
 * calculated — they are defined only as "the age of the holder" and "the year when the holder was
 * born" — so callers should raise an inconsistency as a warning. The comparison anchors at the
 * MSO validFrom, matching the anchor both specifications give for age_over_NN.
 */
public class EnsureMdocAgeInYearsConsistentWithBirthDate
		extends AbstractEnsureMdocAgeElementsConsistentWithBirthDate {

	@Override
	protected String getElementsDescription() {
		return "age_in_years and age_birth_year";
	}

	@Override
	protected int checkAgeElements(String namespace, Map<String, DataItem> elements,
			LocalDate birthDate, int minAge, int maxAge, Map<String, String> problems) {
		int checked = 0;
		DataItem ageInYears = elements.get("age_in_years");
		if (ageInYears instanceof CborInt && ageInYears.getAsNumber() >= 0) {
			checked++;
			long age = ageInYears.getAsNumber();
			if (age < minAge || age > maxAge) {
				problems.put(namespace + " age_in_years", "is " + age + ", but from the birth_date "
					+ birthDate + " the holder was " + (minAge == maxAge ? String.valueOf(minAge)
						: minAge + " to " + maxAge) + " at the MSO validFrom");
			}
		}
		DataItem ageBirthYear = elements.get("age_birth_year");
		if (ageBirthYear instanceof CborInt && ageBirthYear.getAsNumber() >= 0) {
			checked++;
			long year = ageBirthYear.getAsNumber();
			if (year != birthDate.getYear()) {
				problems.put(namespace + " age_birth_year", "is " + year + ", but the birth_date "
					+ birthDate + " is in " + birthDate.getYear());
			}
		}
		return checked;
	}
}
