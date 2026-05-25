package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bcp47SubtagRegistry_UnitTest {

	private final Bcp47SubtagRegistry registry = Bcp47SubtagRegistry.getInstance();

	@Test
	void loadsRegistryAndExposesFileDate() {
		assertNotNull(registry.getFileDate());
	}

	@Test
	void recognisesCommonLanguageSubtags() {
		assertTrue(registry.isRegisteredLanguage("de"));
		assertTrue(registry.isRegisteredLanguage("en"));
		assertTrue(registry.isRegisteredLanguage("zh"));
		assertTrue(registry.isRegisteredLanguage("yue"));
		assertTrue(registry.isRegisteredLanguage("fil"));
	}

	@Test
	void rejectsUnregisteredLanguageSubtags() {
		assertFalse(registry.isRegisteredLanguage("xx"));
		assertFalse(registry.isRegisteredLanguage("zz"));
	}

	@Test
	void languageSubtagsAreCaseSensitive() {
		assertFalse(registry.isRegisteredLanguage("DE"));
		assertFalse(registry.isRegisteredLanguage("En"));
	}

	@Test
	void recognisesCommonRegionSubtags() {
		assertTrue(registry.isRegisteredRegion("DE"));
		assertTrue(registry.isRegisteredRegion("US"));
		assertTrue(registry.isRegisteredRegion("GB"));
		assertTrue(registry.isRegisteredRegion("419"));
	}

	@Test
	void rejectsUnregisteredRegionSubtags() {
		assertFalse(registry.isRegisteredRegion("AB"));
		assertFalse(registry.isRegisteredRegion("ZQ"));
	}

	@Test
	void recognisesCommonScriptSubtags() {
		assertTrue(registry.isRegisteredScript("Latn"));
		assertTrue(registry.isRegisteredScript("Hans"));
		assertTrue(registry.isRegisteredScript("Cyrl"));
	}

	@Test
	void rejectsUnregisteredScriptSubtags() {
		assertFalse(registry.isRegisteredScript("Xxxx"));
	}

	@Test
	void recognisesPrivateUseLanguageRange() {
		assertTrue(registry.isRegisteredLanguage("qaa"));
		assertTrue(registry.isRegisteredLanguage("qtz"));
		assertTrue(registry.isRegisteredLanguage("qmz"));
	}

	@Test
	void recognisesPrivateUseRegionRange() {
		assertTrue(registry.isRegisteredRegion("QM"));
		assertTrue(registry.isRegisteredRegion("QZ"));
		assertTrue(registry.isRegisteredRegion("XA"));
		assertTrue(registry.isRegisteredRegion("XZ"));
	}

	@Test
	void recognisesPrivateUseScriptRange() {
		assertTrue(registry.isRegisteredScript("Qaaa"));
		assertTrue(registry.isRegisteredScript("Qabx"));
	}

	@Test
	void recognisesCommonVariantSubtags() {
		assertTrue(registry.isRegisteredVariant("1901"));
		assertTrue(registry.isRegisteredVariant("1996"));
	}
}
