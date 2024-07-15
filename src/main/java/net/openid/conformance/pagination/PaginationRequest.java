package net.openid.conformance.pagination;

import com.google.common.base.Strings;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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

	public <T> PaginationResponse<T> getResponse(
			Supplier<Long> countQueryAll,
			Function<Pageable, Slice<T>> queryAll,
			Function<String, Long> countQuerySearch,
			BiFunction<String, Pageable, Slice<T>> querySearch) {

		Pageable pageable = getPageable();
		Slice<T> filteredResults = Strings.isNullOrEmpty(search) ? queryAll.apply(pageable) : querySearch.apply('\"' + search + '\"', pageable);

		long totalElementCount = countQueryAll.get();
		long filteredElementCount = Strings.isNullOrEmpty(search) ? totalElementCount : countQuerySearch.apply(search);

		List<T> data = List.copyOf(filteredResults.getContent());
		return new PaginationResponse<>(draw, totalElementCount, filteredElementCount, data);
	}

	private Pageable getPageable() {
		int pageSize = length;
		if (pageSize == 0) {
			pageSize = 10;
		}

		return PageRequest.of(start / pageSize, pageSize, getSort());
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
