package net.openid.conformance.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The paging contract every listing endpoint answers with. Guards the two ways a slice can
 * be supplied - a query pair, and a single query that is told the search term - against
 * drifting apart.
 */
class PaginationRequest_UnitTest {

	private static PaginationRequest request(int draw, int start, int length, String search) {
		PaginationRequest page = new PaginationRequest();
		page.setDraw(draw);
		page.setStart(start);
		page.setLength(length);
		page.setSearch(search);
		return page;
	}

	private static Slice<String> slice(Pageable pageable, boolean hasNext, String... content) {
		return new SliceImpl<>(List.of(content), pageable, hasNext);
	}

	@Test
	void theCountIsSyntheticSoThatNothingHasToBeCounted() {
		PaginationRequest page = request(3, 20, 10, null);

		PaginationResponse<String> response = page.getSliceResponse((search, pageable) ->
			slice(pageable, true, "a", "b", "c"));

		assertThat(response.draw).isEqualTo(3);
		assertThat(response.data).containsExactly("a", "b", "c");
		// there is a next page, so the count claims one more entry than this page can reach
		assertThat(response.recordsTotal).isEqualTo(31);
		assertThat(response.recordsFiltered).isEqualTo(31);
	}

	@Test
	void theLastPageCountsWhatItActuallyHolds() {
		PaginationRequest page = request(0, 20, 10, null);

		PaginationResponse<String> response = page.getSliceResponse((search, pageable) ->
			slice(pageable, false, "a", "b"));

		assertThat(response.recordsTotal).isEqualTo(22);
	}

	@Test
	void aPageIsTenEntriesUnlessAskedOtherwise() {
		List<Pageable> asked = new ArrayList<>();

		request(0, 0, 0, null).getSliceResponse((search, pageable) -> {
			asked.add(pageable);
			return slice(pageable, false);
		});
		request(0, 100, 25, null).getSliceResponse((search, pageable) -> {
			asked.add(pageable);
			return slice(pageable, false);
		});

		assertThat(asked.get(0).getPageSize()).isEqualTo(10);
		assertThat(asked.get(0).getOffset()).isZero();
		assertThat(asked.get(1).getPageSize()).isEqualTo(25);
		assertThat(asked.get(1).getOffset()).isEqualTo(100);
	}

	@Test
	void aSearchTermIsQuotedSoThatItIsOnePhrase() {
		List<String> searched = new ArrayList<>();

		request(0, 0, 10, "fapi ciba").getSliceResponse((search, pageable) -> {
			searched.add(search);
			return slice(pageable, false);
		});
		request(0, 0, 10, "").getSliceResponse((search, pageable) -> {
			searched.add(search);
			return slice(pageable, false);
		});

		assertThat(searched).containsExactly("\"fapi ciba\"", null);
	}

	@Test
	void theQueryPairPicksTheSearchingQueryOnlyWhenThereIsSomethingToSearchFor() {
		List<String> calls = new ArrayList<>();

		request(0, 0, 10, "ciba").getSliceResponse(
			pageable -> {
				calls.add("all");
				return slice(pageable, false);
			},
			(search, pageable) -> {
				calls.add("search " + search);
				return slice(pageable, false);
			});
		request(0, 0, 10, null).getSliceResponse(
			pageable -> {
				calls.add("all");
				return slice(pageable, false);
			},
			(search, pageable) -> {
				calls.add("search " + search);
				return slice(pageable, false);
			});

		assertThat(calls).containsExactly("search \"ciba\"", "all");
	}
}
