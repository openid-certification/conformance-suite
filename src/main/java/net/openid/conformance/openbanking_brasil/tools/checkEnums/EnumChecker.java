package net.openid.conformance.openbanking_brasil.tools.checkEnums;

import net.openid.conformance.util.field.Field;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
			field.getEnums().forEach(value -> {
				if (value.length() > field.getMaxLength()) {
					logger.warn("Max length inconsistency | {} | {} | {} | {} | {}", validatorClassName,
						field.getPath(), value,	value.length(), field.getMaxLength());
				}
			});
		} else {
			logger.warn("Max length is indefined | {} | {} | {}",
				validatorClassName,	field.getPath(), field.getMaxLength());
		}
	}

}



