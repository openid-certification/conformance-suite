package net.openid.conformance.openbanking_brasil.tools.checkEnums;

import net.openid.conformance.util.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumChecker {
	private static final Logger logger = LoggerFactory.getLogger("enum-checker");
	private static final EnumChecker instance = new EnumChecker();

	private EnumChecker() {
	}

	public static EnumChecker getInstance() {
		return instance;
	}

	public void check(Field field, String validatorClassName) {
		if (field.getMaxLength() > 0) {
			int maxLengthCalculated = 0;
			for (String value : field.getEnums()) {
				maxLengthCalculated = Math.max(maxLengthCalculated, value.length());
				if (value.length() > field.getMaxLength()) {
					logger.error("Max length inconsistency (error) | {} | {} | {} | {} | {}", validatorClassName,
						field.getPath(), value, value.length(), field.getMaxLength());
				}
			}
			if (field.getMaxLength() != maxLengthCalculated) {
				logger.warn("Max length inconsistency (warn) | {} | {} | {} | change to {}", validatorClassName,
					field.getPath(), field.getMaxLength(), maxLengthCalculated);
			}
		} else {
			logger.warn("Max length is indefined (warn) | {} | {} | {}",
				validatorClassName,	field.getPath(), field.getMaxLength());
		}
	}
}
