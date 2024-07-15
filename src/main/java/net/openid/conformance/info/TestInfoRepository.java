package net.openid.conformance.info;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Map;
import java.util.Optional;

public interface TestInfoRepository extends PagingAndSortingRepository<TestInfo, String>, CrudRepository<TestInfo, String> {

	Slice<TestInfo> findAllBy(Pageable pageable);

	@Query(value = "{ $text: { $search: ?0 } }", count = true)
	Long countAllSearch(String search);

	@Query("{ $text: { $search: ?0 } }")
	Slice<TestInfo> findAllSearch(String search, Pageable pageable);

	@Query("{ owner: ?0 }")
	Iterable<TestInfo> findAllByOwner(Map<String, String> owner);

	@Query(value = "{ owner: ?0 }", count = true)
	Long countAllByOwner(Map<String, String> owner);

	@Query("{ owner: ?0 }")
	Slice<TestInfo> findAllByOwner(Map<String, String> owner, Pageable pageable);

	@Query(value = "{ owner: ?0, $text: { $search: ?1 } }", count = true)
	Long countAllByOwnerSearch(Map<String, String> owner, String search);

	@Query("{ owner: ?0, $text: { $search: ?1 } }")
	Slice<TestInfo> findAllByOwnerSearch(Map<String, String> owner, String search, Pageable pageable);

	@Query(value = "{ publish: { $in: [ 'summary', 'everything' ] } }", count = true)
	Long countAllPublic();

	@Query("{ publish: { $in: [ 'summary', 'everything' ] } }")
	Slice<PublicTestInfo> findAllPublic(Pageable pageable);

	@Query(value = "{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }", count = true)
	Long countAllPublicSearch(String search);

	@Query("{ publish: { $in: [ 'summary', 'everything' ] }, $text: { $search: ?0 } }")
	Slice<PublicTestInfo> findAllPublicSearch(String search, Pageable pageable);

	@Query("{ _id: ?0, owner: ?1 }")
	Optional<TestInfo> findByIdAndOwner(String id, Map<String, String> owner);

	@Query("{ _id: ?0, publish: { $in: [ 'summary', 'everything' ] } }")
	Optional<PublicTestInfo> findByIdPublic(String id);
}
