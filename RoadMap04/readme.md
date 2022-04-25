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

### 확장 기능

##### 사용자 정의 Repository 구현
- 새로운 interface 생성
```
public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}
```
- repository명+Impl 으로 된 class 생성(+ 사용자 정의 인터페이스 명 + Impl)
```
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}
```
- 기존 interface 에 추가 implements
```
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
  ...
}
```
- 꼭 interface 명에 Impl 붙여서 class 생성해야함
```
// 설정 변경 방법
@EnableJpaRepositories(basePackages = "study.datajpa.repository", repositoryImplementationPostfix = "Impl")
```

##### Auditing
- 순수 JPA 사용
  - `@MappedSuperclass` : Entity가 아닌 부모객체 등록
  - `@PrePersist`, `@PostPersist` : persist 동작 전/후
  - `@PreUpdate`, `@PostUpdate` : update 동작 전/후
```
@MappedSuperclass
@Getter
public class JpaBaseEntity {

    @Column(updatable = false)
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createDate = now;
        this.updateDate = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDate = LocalDateTime.now();
    }

}
```

- 스프링 데이터 JPA 사용
  - `@EnableJpaAuditing` - 스프링 부트 설정 클래스에 적용해야함
  - `@EntityListeners(AuditingEntityListener.class)` - 엔티티에 적용
  - `@CreatedDate`, `@LastModifiedDate`,`@CreatedBy`, `@LastModifiedBy`
```
@EnableJpaAuditing
@SpringBootApplication
public class DataJpaApplication {
  ...
}

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```
- 전체 적용 방법
  - @EntityListeners(AuditingEntityListener.class) 를 생략하고 일괄 적용
  - META-INF/orm.xml 파일 생성
```
<?xml version=“1.0” encoding="UTF-8”?>
<entity-mappings xmlns=“http://xmlns.jcp.org/xml/ns/persistence/orm”
  xmlns:xsi=“http://www.w3.org/2001/XMLSchema-instance”
  xsi:schemaLocation=“http://xmlns.jcp.org/xml/ns/persistence/
  orm http://xmlns.jcp.org/xml/ns/persistence/orm_2_2.xsd”
  version=“2.2">
  <persistence-unit-metadata>
    <persistence-unit-defaults>
      <entity-listeners>
        <entity-listener class="org.springframework.data.jpa.domain.support.AuditingEntityListener”/>
      </entity-listeners>
    </persistence-unit-defaults>
  </persistence-unit-metadata>
</entity-mappings>
```

##### Web 확장 - 도메인 클래스 컨버터
- HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩
- 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
- 도메인 클래스 컨버터로 엔티티를 파라미터로 받으면, 이 엔티티는 단순 조회용으로만 사용해야 함(트랜잭션 범위 밖)
```
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }

    @GetMapping("/members/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }
}
```

##### Web 확장 - 페이징과 정렬
- 스프링 데이터가 제공하는 페이징과 정렬 기능을 스프링 MVC에서 편리하게 사용할 수 있음
- 파라미터로 Pageable 을 받을 수 있음
- 요청 파라미터
  - page: 현재 페이지, 0부터 시작
  - size: 한 페이지에 노출할 데이터 건수
  - sort: 정렬 조건
  - ex) /members?page=0&size=3&sort=id,desc&sort=username,desc
```
@GetMapping("/members")
public Page<Member> list(Pageable pageable) {
  Page<Member> page = memberRepository.findAll(pageable);
  return page;
}
```
- 글로벌 설정: 스프링 부트
  - spring.data.web.pageable.default-page-size=20 /# 기본 페이지 사이즈/
  - spring.data.web.pageable.max-page-size=2000 /# 최대 페이지 사이즈/
- 개별 설정
  - `@PageableDefault` 어노테이션
```
@GetMapping("/members")
public Page<Member> list(@PageableDefault(size = 5, sort = {"username"}) Pageable pageable) {
    Page<Member> page = memberRepository.findAll(pageable);
    return page;
}
```
- 접두사
  - 페이징 정보가 둘 이상이면 접두사로 구분
  - `@Qualifier` 에 접두사명 추가 "{접두사명}_xxx”
```
public String list(
       @Qualifier("member") Pageable memberPageable,
       @Qualifier("order") Pageable orderPageable, ..
```
- Page 내용을 DTO로 변환
```
@GetMapping("/members")
public Page<MemberDto> list(@PageableDefault(size = 5, sort = {"username"}) Pageable pageable) {
    Page<Member> page = memberRepository.findAll(pageable);
    Page<MemberDto> map = page.map(MemberDto::new);
    return map;
}
```
- Page를 1부터 시작하기
  - 직접 클래스를 만들어서 처리
  - spring.data.web.pageable.one-indexed-parameters : true 설정
    - 웹에서 page 객체를 -1 처리할 뿐, 응답 시 -1 처리가 안됨

### 스프링 데이터 JPA 분석

##### 스프링 데이터 JPA 구현체 분석
- JpaRepository 인터페이스의 구현체: org.springframework.data.jpa.repository.support.SimpleJpaRepository
- `@Repository` 적용: JPA 예외를 스프링이 추상화한 예외로 변환
- `@Transactional` 트랜잭션 적용
  - 서비스 계층에서 트랜잭션을 시작하면 리파지토리는 해당 트랜잭션을 전파 받아서 사용
  - SpringDataJpa 내부적으로 트랜잭션이 있음
  - `@Transactional(readOnly = true)`
    - 데이터를 단순히 조회만 하고 변경하지 않는 트랜잭션에서 `readOnly = true` 옵션을 사용하면 플러시를 생략해서 약간의 성능 향상을 얻을 수 있음
- `save` 메소드는 새로운 엔티티면 저장(persist), 새로운 엔티티가 아니면 병합(merge)

##### 새로운 엔티티를 구별하는 방법
- 새로운 엔티티를 판단하는 기본 전략
  - 식별자가 객체일 때 null 로 판단
  - 식별자가 자바 기본 타입일 때 0 으로 판단
  - `Persistable` 인터페이스를 구현해서 판단 로직 변경 가능
- tip: `@CreatedDate` 이용
```
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item implements Persistable<String> {

  @Id
  private String id;
  
  @CreatedDate
  private LocalDateTime createdDate;
  
  public Item(String id) {
    this.id = id;
  }
  
  @Override
  public String getId() {
    return id;
  }
  
  @Override
  public boolean isNew() {
    return createdDate == null;
  }
}
```

### 나머지 기능들

##### Specifications (명세)
- `org.springframework.data.jpa.domain.Specification` 클래스로 정의
- `JpaSpecificationExecutor` 인터페이스 상속
- Specification 을 구현하면 명세들을 조립할 수 있음. where() , and() , or() , not() 제공
- 사용 방법이 불편하므로 실무에서 사용 비추천!

##### Query By Example
- Probe: 필드에 데이터가 있는 실제 도메인 객체
- ExampleMatcher: 특정 필드를 일치시키는 상세한 정보 제공, 재사용 가능
- Example: Probe와 ExampleMatcher로 구성, 쿼리를 생성하는데 사용
```
//Probe
Member member = new Member("m1");
Team team = new Team("teamA");
member.setTeam(team);

//ExampleMatcher
ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("age");

//Example
Example<Member> example = Example.of(member, matcher);

List<Member> result = memberRepository.findAll(example);
```
- 장점
  - 동적 쿼리를 편리하게 처리
  - 도메인 객체를 그대로 사용
  - 데이터 저장소를 RDB에서 NOSQL로 변경해도 코드 변경이 없게 추상화 되어 있음
  - 스프링 데이터 JPA JpaRepository 인터페이스에 이미 포함
- 단점
  - 조인은 가능하지만 내부 조인(INNER JOIN)만 가능함 외부 조인(LEFT JOIN) 안됨
  - 중첩 제약조건 안됨
  - 매칭 조건이 매우 단순함
    - 문자는 starts/contains/ends/regex
    - 다른 속성은 정확한 매칭( = )만 지원
- 매칭 조건이 너무 단순하고, LEFT 조인이 안되므로 실무에서 사용 비추천!

##### Projections
- 엔티티 대신에 DTO를 편리하게 조회할 때 사용
- 조회할 엔티티의 필드를 getter 형식으로 지정하면 해당 필드만 선택해서 조회
```
public interface UsernameOnly {
  String getUsername();
}
```
- 메서드 이름은 자유, 반환 타입으로 인지
```
public interface MemberRepository ... {
  List<UsernameOnly> findProjectionsByUsername(String username);
}
```
- 인터페이스 기반 Closed Projections
```
public interface UsernameOnly {
  String getUsername();
}
```
- 인터페이스 기반 Open Proejctions
```
public interface UsernameOnly {
  @Value("#{target.username + ' ' + target.age + ' ' + target.team.name}")
  String getUsername();
}
```
- 클래스 기반 Projection
```
public class UsernameOnlyDto {
  private final String username;
  
  public UsernameOnlyDto(String username) {
    this.username = username;
  }
  
  public String getUsername() {
    return username;
  }
}
```
- 동적 Projections
```
<T> List<T> findProjectionsByUsername(String username, Class<T> type);

List<UsernameOnly> result = memberRepository.findProjectionsByUsername("m1", UsernameOnly.class);
```
- 중첩 구조 처리
```
public interface NestedClosedProjection {
  String getUsername();
  
  TeamInfo getTeam();
  
  interface TeamInfo {
    String getName();
  }
}
```
- 주의
  - 프로젝션 대상이 root 엔티티면, JPQL SELECT 절 최적화 가능
  - 프로젝션 대상이 ROOT가 아니면 LEFT OUTER JOIN 처리 후 모든 필드를 SELECT해서 엔티티로 조회한 다음에 계산
- 실무에서는 단순할 때만 사용하고, 조금만 복잡해지면 비추천!

##### 네이티브 쿼리
- 페이징 지원
- 반환 타입
  - Object[]
  - Tuple
  - DTO(스프링 데이터 인터페이스 Projections 지원)
- 단점
  - Sort 파라미터를 통한 정렬이 정상 동작하지 않을 수 있음
  - JPQL처럼 애플리케이션 로딩 시점에 문법 확인 불가
  - 동적 쿼리 불가
- JPA 네이티브 SQL 지원
  - DTO로 변환하려면 사용 불편
    - DTO 대신 JPA TUPLE 조회
    - DTO 대신 MAP 조회
    - @SqlResultSetMapping
    - Hibernate ResultTransformer를 사용
```
@Query(value = "select * from member where username = ?", nativeQuery = true)
Member findByNativeQuery(String username);
```
- Projections 활용
```
@Query(value = "select m.member_id as id, m.username, t.name as teamName from member m left join team t"
        , countQuery = "select count(*) from member"
        , nativeQuery = true)
Page<MemberProjection> findByNativeProjection(Pageable pageable);
```
- 동적 네이티브 쿼리
  - 하이버네이트를 직접 활용
  - 스프링 JdbcTemplate, myBatis, jooq같은 외부 라이브러리 사용 권장
```
String sql = "select m.username as username from member m";

List<MemberDto> result = em.createNativeQuery(sql)
                            .setFirstResult(0)
                            .setMaxResults(10)
                            .unwrap(NativeQuery.class)
                            .addScalar("username")
                            .setResultTransformer(Transformers.aliasToBean(MemberDto.class))
                            .getResultList();
```
