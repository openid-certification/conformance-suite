package net.openid.conformance.condition.client;

import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Simple;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that every age_over_NN data element agrees with the mdoc's birth_date. ISO/IEC 18013-5
 * 13.4.6 and ISO/IEC TS 23220-2 6.3.2.2 require an age_over_NN value to be valid at the MSO
 * validFrom, so an inconsistency violates a "shall" and callers should treat it as a failure.
 */
public class EnsureMdocAgeOverElementsConsistentWithBirthDate
		extends AbstractEnsureMdocAgeElementsConsistentWithBirthDate {

	private static final Pattern AGE_OVER_NN = Pattern.compile("age_over_(\\d\\d)");

	@Override
	protected String getElementsDescription() {
		return "age_over_NN";
	}

	@Override
	protected int checkAgeElements(String namespace, Map<String, DataItem> elements,
			LocalDate birthDate, int minAge, int maxAge, Map<String, String> problems) {
		int checked = 0;
		for (Map.Entry<String, DataItem> element : elements.entrySet()) {
			Matcher matcher = AGE_OVER_NN.matcher(element.getKey());
			if (!matcher.matches()) {
				continue;
			}
			DataItem value = element.getValue();
			boolean isTrue = Simple.Companion.getTRUE().equals(value);
			boolean isFalse = Simple.Companion.getFALSE().equals(value);
			if (!isTrue && !isFalse) {
				// not a bool; the element value checks report that
				continue;
			}
			checked++;
			int nn = Integer.parseInt(matcher.group(1));
			if (isTrue && nn > maxAge) {
				problems.put(namespace + " " + element.getKey(), "is true, but from the birth_date "
					+ birthDate + " the holder was at most " + maxAge + " at the MSO validFrom");
			} else if (isFalse && nn <= minAge) {
				problems.put(namespace + " " + element.getKey(), "is false, but from the birth_date "
					+ birthDate + " the holder was at least " + minAge + " at the MSO validFrom");
			}
		}
		return checked;
	}
}
