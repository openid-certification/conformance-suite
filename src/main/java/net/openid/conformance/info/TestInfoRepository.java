package net.openid.conformance.info;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TestInfoRepository extends PagingAndSortingRepository<TestInfo, String>, CrudRepository<TestInfo, String> {

	@Query("{}")
	Slice<TestInfo> findAllAsSlice(Pageable pageable);

	@Query("{ $text: { $search: ?0 } }")
	Slice<TestInfo> findAllSearchAsSlice(String search, Pageable pageable);

	@Query("{ owner: ?0 }")
	Slice<TestInfo> findAllByOwnerAsSlice(Map<String, String> owner, Pageable pageable);

	@Query("{ owner: ?0, $text: { $search: ?1 } }")
	Slice<TestInfo> findAllByOwnerSearchAsSlice(Map<String, String> owner, String search, Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] } }")
	Slice<PublicTestInfo> findAllPublicAsSlice(Pageable pageable);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }")
	Slice<PublicTestInfo> findAllPublicSearchAsSlice(String search, Pageable pageable);

	@Query("{ _id: ?0, owner: ?1 }")
	Optional<TestInfo> findByIdAndOwner(String id, Map<String, String> owner);

	@Query("{ _id: ?0, publish: { $in: [ 'summary', 'everything' ] } }")
	Optional<PublicTestInfo> findByIdPublic(String id);

	@Query("{ _id: { $in: ?0 } }")
	List<TestInfo> findAllByIdIn(Collection<String> ids);

	@Query("{ _id: { $in: ?0 }, owner: ?1 }")
	List<TestInfo> findAllByIdInAndOwner(Collection<String> ids, Map<String, String> owner);

	@Query("{ _id: { $in: ?0 }, publish: { $in: [ 'summary', 'everything' ] } }")
	List<PublicTestInfo> findAllByIdInPublic(Collection<String> ids);
}
