package net.openid.conformance.pagination;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.util.function.BiFunction;
import java.util.function.Function;

public class PaginationRequest {

	private int draw;

	private int start;

	private int length;

	private String search;

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

		Slice<T> results = Strings.isNullOrEmpty(search)
				? queryAll.apply(getPageable())
				: querySearch.apply('\"' + search + '\"', getPageable());

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
