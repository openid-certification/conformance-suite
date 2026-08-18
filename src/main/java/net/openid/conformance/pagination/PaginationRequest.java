package net.openid.conformance.pagination;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.util.function.BiFunction;
import java.util.function.Function;

public class PaginationRequest {

	@Schema(description = "DataTables echo counter; returned unchanged as 'draw' in the response so a client can match responses to requests")
	private int draw;

	@Schema(description = "0-based index of the first record to return")
	private int start;

	@Schema(description = "Page size; 0 (or unset) means 10, values above 1000 are rejected")
	private int length;

	@Schema(description = "Free-text search term, matched with a MongoDB text search")
	private String search;

	@Schema(description = "Sort specification: a flat comma-separated list of column,direction pairs, e.g. 'started,desc'. Direction is 'asc' unless it is exactly 'desc'.", example = "started,desc")
	private String order;

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		if (length > 1000) {
			throw new RuntimeException("Page length in excess of 1000 entries requested.");
		}
		this.length = length;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public <T> PaginationResponse<T> getSliceResponse(
			Function<Pageable, Slice<T>> queryAll,
			BiFunction<String, Pageable, Slice<T>> querySearch) {

		return getSliceResponse((term, pageable) -> term == null
				? queryAll.apply(pageable)
				: querySearch.apply(term, pageable));
	}

	/**
	 * @param query is given the term to search for - quoted, so that it is one phrase, or null
	 *              when nothing was searched for - and the page to return
	 * @return what it returned, in the paging envelope the listing endpoints answer with
	 */
	public <T> PaginationResponse<T> getSliceResponse(BiFunction<String, Pageable, Slice<T>> query) {

		Slice<T> results = query.apply(
				Strings.isNullOrEmpty(search) ? null : '\"' + search + '\"',
				getPageable());

		int pageLength = length == 0 ? 10 : length;
		long syntheticCount = results.hasNext()
				? (long) start + pageLength + 1
				: (long) start + results.getNumberOfElements();

		return new PaginationResponse<>(draw,
				syntheticCount,
				syntheticCount,
				Lists.newArrayList(results));
	}

	private Pageable getPageable() {
		int l = length;
		if (l == 0) {
			l = 10;
		}

		return PageRequest.of(start / l, l, getSort());
	}

	private Sort getSort() {

		Sort sort = Sort.unsorted();

		if (order != null) {
			String[] orderParts = order.split(",");
			for (int i = 0; i < orderParts.length; i += 2) {
				String column = orderParts[i];
				String dir = (i + 1 < orderParts.length) ? orderParts[i + 1] : "asc";
				Order order = dir.equals("desc") ? Sort.Order.desc(column) : Sort.Order.asc(column);
				sort = sort.and(Sort.by(order));
			}
		}

		return sort;
	}
}
