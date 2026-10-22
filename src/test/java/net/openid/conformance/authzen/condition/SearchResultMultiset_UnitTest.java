package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultMultiset_UnitTest {

	private static JsonArray array(String json) {
		return JsonParser.parseString(json).getAsJsonArray();
	}

	private static JsonElement element(String json) {
		return JsonParser.parseString(json);
	}

	@Test
	public void sameEntities_ignoresOrderAndProperties() {
		assertTrue(SearchResultMultiset.sameEntities(
			array("[ {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"x\":1}}, {\"type\":\"user\",\"id\":\"bob\"} ]"),
			array("[ {\"id\":\"bob\",\"type\":\"user\"}, {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"x\":2}} ]")));
	}

	@Test
	public void sameEntities_countsDuplicates() {
		assertFalse(SearchResultMultiset.sameEntities(
			array("[ {\"type\":\"user\",\"id\":\"alice\"}, {\"type\":\"user\",\"id\":\"alice\"} ]"),
			array("[ {\"type\":\"user\",\"id\":\"alice\"}, {\"type\":\"user\",\"id\":\"bob\"} ]")));
	}

	@Test
	public void sameEntities_differentSizes_isFalse() {
		assertFalse(SearchResultMultiset.sameEntities(
			array("[ {\"type\":\"user\",\"id\":\"alice\"} ]"),
			array("[ {\"type\":\"user\",\"id\":\"alice\"}, {\"type\":\"user\",\"id\":\"bob\"} ]")));
	}

	@Test
	public void sameResults_comparesProperties() {
		assertFalse(SearchResultMultiset.sameResults(
			array("[ {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"x\":1}} ]"),
			array("[ {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"x\":2}} ]")));
		assertTrue(SearchResultMultiset.sameResults(
			array("[ {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"x\":1}} ]"),
			array("[ {\"properties\":{\"x\":1},\"id\":\"alice\",\"type\":\"user\"} ]")));
	}

	@Test
	public void countIdentifying_matchesGivenMembersOnly() {
		JsonArray results = array("[ {\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"role\":\"admin\"}}, "
			+ "{\"type\":\"user\",\"id\":\"alice\"}, {\"type\":\"user\",\"id\":\"bob\"} ]");
		assertEquals(2, SearchResultMultiset.countIdentifying(results, element("{\"type\":\"user\",\"id\":\"alice\",\"properties\":{\"role\":\"other\"}}")));
		assertEquals(1, SearchResultMultiset.countIdentifying(results, element("{\"type\":\"user\",\"id\":\"bob\"}")));
		assertEquals(0, SearchResultMultiset.countIdentifying(results, element("{\"type\":\"record\",\"id\":\"bob\"}")));
	}

	@Test
	public void countIdentifying_actionByName() {
		JsonArray results = array("[ {\"name\":\"read\"}, {\"name\":\"write\",\"properties\":{}} ]");
		assertEquals(1, SearchResultMultiset.countIdentifying(results, element("{\"name\":\"write\"}")));
		assertEquals(0, SearchResultMultiset.countIdentifying(results, element("{\"name\":\"delete\"}")));
	}

	@Test
	public void countIdentifying_nonObjectResult_isComparedAsAWhole() {
		assertEquals(0, SearchResultMultiset.countIdentifying(array("[ \"alice\" ]"), element("{\"type\":\"user\",\"id\":\"alice\"}")));
		assertEquals(1, SearchResultMultiset.countIdentifying(array("[ \"alice\" ]"), element("\"alice\"")));
	}
}
