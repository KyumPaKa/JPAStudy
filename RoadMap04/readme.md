# 실전! 스프링 데이터 JPA

- SpringDataJPA 생성
  - T: 엔티티 타입
  - ID: 식별자 타입(PK)
```
public interface TeamRepository extends JpaRepository<T, ID> {
    ...
}
```

### 쿼리 메소드 기능
- 쿼리 메소드 기능 3가지
  - 메소드 이름으로 쿼리 생성
  - 메소드 이름으로 JPA `@NamedQuery` 호출
  - `@Query` 어노테이션을 사용해서 리파지토리 인터페이스에 쿼리 직접 정의

##### 메소드 이름으로 쿼리 생성
- 메소드 이름을 분석해서 JPQL을 생성하고 실행
- 조회: find…By ,read…By ,query…By get…By
- COUNT: count…By 반환타입 `long`
- EXISTS: exists…By 반환타입 `boolean`
- 삭제: delete…By, remove…By 반환타입 `long`
- DISTINCT: findDistinct, findMemberDistinctBy
- LIMIT: findFirst3, findFirst, findTop, findTop3

##### JPA NamedQuery
```
@Entity
@NamedQuery(
    name="Member.findByUsername",
    query="select m from Member m where m.username = :username")
public class Member {
    ...
}

// JPA를 직접 사용해서 Named 쿼리 호출
public class MemberRepository {
    public List<Member> findByUsername(String username) {
        ...
        List<Member> resultList = em.createNamedQuery("Member.findByUsername", Member.class)
                                    .setParameter("username", username)
                                    .getResultList();
    }
}

// 스프링 데이터 JPA로 NamedQuery 사용
@Query(name = "Member.findByUsername")
List<Member> findByUsername(@Param("username") String username);

// 스프링 데이터 JPA로 Named 쿼리 호출
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByUsername(@Param("username") String username);
}
```

##### @Query, 리포지토리 메소드에 쿼리 정의하기
```
@Query("select m from Member m where m.username= :username and m.age = :age")
List<Member> findUser(@Param("username") String username, @Param("age") int age);
```

##### @Query, 값, DTO 조회하기
```
// 단순히 값 하나를 조회
@Query("select m.username from Member m")
List<String> findUsernameList();

// DTO로 직접 조회
@Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
List<MemberDto> findMemberDto();
```

##### 파라미터 바인딩
- 위치 기반
- 이름 기반
```
select m from Member m where m.username = ?0 //위치 기반
select m from Member m where m.username = :name //이름 기반
```

##### 반환 타입
```
List<Member> findByUsername(String name); //컬렉션
Member findByUsername(String name); //단건
Optional<Member> findByUsername(String name); //단건 Optional
```

- 조회 결과가 많거나 없으면 반환 방법
- 컬렉션
  - 결과 없음: 빈 컬렉션 반환
- 단건 조회
  - 결과 없음: null 반환
  - 결과가 2건 이상: `javax.persistence.NonUniqueResultException` 예외 발생

##### 순수 JPA 페이징과 정렬
```
public List<Member> findByPage(int age, int offset, int limit) {
    return em.createQuery("select m from Member m where m.age = :age order by m.username desc")
            .setParameter("age", age)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
}

public long totalCount(int age) {
    return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
            .setParameter("age", age)
            .getSingleResult();
}
```

##### 스프링 데이터 JPA 페이징과 정렬
- 페이징과 정렬 파라미터
  - `org.springframework.data.domain.Sort` : 정렬 기능
  - `org.springframework.data.domain.Pageable` : 페이징 기능
- 특별한 반환 타입
  - `org.springframework.data.domain.Page` : 추가 count 쿼리 결과를 포함하는 페이징
  - `org.springframework.data.domain.Slice` : 추가 count 쿼리 없이 다음 페이지만 확인 가능(내부적으로 limit + 1조회)
  - List : 추가 count 쿼리 없이 결과만 반환
```
Page<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용
Slice<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함
List<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함
List<Member> findByUsername(String name, Sort sort);
```

##### 벌크성 수정 쿼리
- 벌크 연산은 영속성 컨텍스트를 무시하고 실행하기 때문에 사용에 유의해야함
- JPA를 사용한 벌크성 수정 쿼리
```
public int bulkAgePlus(int age) {
    int resultCount = em.createQuery("update Member m set m.age = m.age + 1 where m.age >= :age")
                        .setParameter("age", age)
                        .executeUpdate();
    return resultCount;
}
```
- 스프링 데이터 JPA를 사용한 벌크성 수정 쿼리
```
@Modifying
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkAgePlus(@Param("age") int age);
```

##### @EntityGraph
- 연관된 엔티티들을 SQL 한번에 조회하는 방법
- 사실상 페치 조인(FETCH JOIN)의 간편 버전
- LEFT OUTER JOIN 사용
```
//공통 메서드 오버라이드
@Override
@EntityGraph(attributePaths = {"team"})
List<Member> findAll();

//JPQL + 엔티티 그래프
@EntityGraph(attributePaths = {"team"})
@Query("select m from Member m")
List<Member> findMemberEntityGraph();

//메서드 이름으로 쿼리에서 특히 편리하다.
@EntityGraph(attributePaths = {"team"})
List<Member> findByUsername(String username)
```

##### JPA Hint & Lock
- JPA 쿼리 힌트(SQL 힌트가 아니라 JPA 구현체에게 제공하는 힌트)
- forCounting : 반환 타입으로 Page 인터페이스를 적용하면 추가로 호출하는 페이징을 위한 count 쿼리도 쿼리 힌트 적용(기본값 true )
```
@QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
Member findReadOnlyByUsername(String username);
```
- Lock
```
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Member> findByUsername(String name);
```







