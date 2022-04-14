# 인프런 강의(자바 ORM 표준 JPA 프로그래밍 - 기본편, 김영한) 학습

### SQL 중심적인 개발의 문제점
- CRUD와 같은 반복적인 SQL, 코드 작성
- 객체의 필드 변경시 SQL 변경에 주의 필요
- 객체와 관계형 데이터베이스의 차이
  - 상속
  - 연관관계
  - 데이터 타입
  - 데이터 식별 방법
- 객체 구현일 경우 다형성도 활용 가능
```
// 자식 타입
Album album = list.get(albumId);
// 부모 타입
Item item = list.get(albumId);
```
- 객체는 참조를 사용, DB는 외래키를 사용 <br>
객체: member.getTeam();
SQL: JOIN ON M.TEAM_ID = T.TEAM_ID
- SQL에 따라 탐색 범위 결정, 객체는 자유롭게 객체 그래프를 탐색 가능해야함
```
SELECT M.*, T.*
  FROM MEMBER M
  JOIN TEAM T ON M.TEAM_ID = T.TEAM_ID

member.getTeam(); // OK
member.getOrder(); // null
```
- 엔티티 신뢰 문제 -> 상황에 따른 모든 조회 메서드 생성해야함
```
class MemberService {
  ...
  public void process() {
    Member member = memberDao.find(memberId);
    member.getTeam(); // ??
    member.getOrder().getDelivery(); // ??
  }
}
```
- 계층형 아키텍처, 진정한 의미의 계층 분할이 어려움
- SQL 조회 결과 같은 값이지만 다르다고 표현됨
```
String memberId = "100";
Member member1 = memberDao.getMember(memberId);
Member member2 = memberDao.getMember(memberId);
member1 == member2; // 다르다
```
```
String memberId = "100";
Member member1 = list.get(memberId);
Member member2 = list.get(memberId);
member1 == member2; // 같다
```

### JPA 소개
- JPA
  - Java Persistence API
  - 자바 진영의 ORM 기술 표준
  - 인터페이스의 모음
  - 구현체: Hibernate, EclipseLink, DataNucleus

- ORM
  - Object-relational mapping(객체 관계 매핑)
  - 객체는 객체대로, 관계형 데이터베이스는 관계형 데이터베이스대로 설계 후 ORM이 중간에서 매핑
  - 대중적인 언어에는 대부분 ORM 기술이 존재

- JPA 사용해야하는 이유
  - SQL 중심적인 개발에서 객체 중심 으로 개발
  - 생산성
  - 유지보수
  - 패러다임 불일치 해결
  - 성능
    - 1차 캐시와 동일성 보장
    - 트랜잭션을 지원하는 쓰기 지연
    - 지연 로딩
  - 데이터 접근 추상화와 벤더 독립성

### JPA 기초
- JPA 설정
  - persistence.xml 정보로 설정
  - /META-INF/persistence.xml 위치해야함
  - javax.persistence로 시작: JPA 표준 속성
  - hibernate로 시작: 하이버네이트 전용 속성
```
<!-- 필수 속성 -->
<property name="javax.persistence.jdbc.driver" value="org.h2.Driver"/>
<property name="javax.persistence.jdbc.user" value="sa"/>
<property name="javax.persistence.jdbc.password" value=""/>
<property name="javax.persistence.jdbc.url" value="jdbc:h2:tcp://localhost/~/test"/>
<property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>

<!-- 옵션 -->
<property name="hibernate.show_sql" value="true"/>
<property name="hibernate.format_sql" value="true"/>
<property name="hibernate.use_sql_comments" value="true"/>
<!--<property name="hibernate.hbm2ddl.auto" value="create" />-->
```

- 방언(dialect)
  - SQL 표준을 지키지 않는 특정 데이터베이스만의 고유한 기능
  - JPA는 특정 데이터베이스에 종속되지 않음
  - 각각의 데이터베이스가 제공하는 SQL 문법과 함수는 조금씩 다름
  - 하이버네이트는 40가지 이상의 데이터베이스 방언 지원

- 기본 어노테이션
  - @Entity: JPA가 관리할 객체
  - @Id: 데이터베이스 PK와 매핑
  - @Table: 테이블명 지정
  - @Column: 컬럼명 지정

- 엔티티 매니저 팩토리는 하나만 생성해서 애플리케이션 전체에서 공유
- 엔티티 매니저는 쓰레드간에 공유하면 안됨
- JPA의 모든 데이터 변경은 트랜잭션 안에서 실행해야함
```
// persistence.xml에 설정된 persistence-unit의 name으로 EntityManagerFactory 설정
EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

EntityManager em = emf.createEntityManager();

EntityTransaction tx = em.getTransaction();
tx.begin();

try {
    ...
    
    tx.commit();
} catch (Exception ex) {
    tx.rollback();
} finally {
    em.close();
}

emf.close();
```

- JPQL
  - 검색을 할 때도 테이블이 아닌 엔티티 객체를 대상으로 검색
  - 애플리케이션이 필요한 데이터만 DB에서 불러오려면 결국 검색 조건이 포함된 SQL이 필요
  - JPA는 SQL을 추상화한 JPQL이라는 객체 지향 쿼리 언어 제공
  - 테이블이 아닌 객체를 대상으로 검색하는 객체 지향 쿼리
  - SQL과 문법 유사함
    - SELECT, FROM, WHERE, GROUP BY, HAVING, JOIN 지원
  - JPQL은 엔티티 객체를 대상으로 쿼리, SQL은 데이터베이스 테이블을 대상으로 쿼리
```
List<Member> result = em.createQuery("select m from Member as m", Member.class)
        .setFirstResult(0)
        .setMaxResults(1)
        .getResultList();
```

### 영속성 컨텍스트
- 영속성 컨텍스트
  - 엔티티를 영구 저장하는 환경
  - EntityManager.persist(entity) -> DB가 아닌 영속성 컨텍스트에 저장하는 것
  - 논리적인 개념
  - 엔티티매니저를 통해 영속성 컨텍스트에 접근

- 엔티티 생명주기
  - 영속성(new/transient): 영속성 컨텍스트와 전혀 관계가 없는 새로운 상태
  - 영속(managed): 영속성 컨텍스트에 관리되는 상태
  - 준영속(detached): 영속성 컨텍스트에 저장되었다가 분리된 상태
  - 삭제(removed): 삭제된 상태
```
// 비영속
Member member = new Member();
member.setId(100L);
member.setName("HelloJPA");

// 영속
em.persist(member);

// 회원 엔티티를 영속성 컨텍스트에서 분리, 준영속 상태
em.detach(member);

// 객체를 삭제한 상태
em.remove(member);
```

- 영속성 컨텍스트의 이점
  - 1차 캐시
  - 동일성 보장
  - 트랜잭션을 지원하는 쓰기 지연
  - 변경 감지
  - 지연 로딩

- 플러시
  - 영속성 컨텍스트의 변경내용을 데이터베이스에 반영
  - 플러시 발생
    - 변경 감지
    - 수정된 엔티티 쓰기 지연 SQL 저장소에 등록
    - 쓰기 지연 SQL 저장소의 쿼리를 데이터베이스에 전송
  - 사용 방법
    - em.flush();
    - 트랜잭션 커밋
    - JPQL 쿼리 실행
  - 플러시 모드 옵션
    - em.setFlushMode(FlushModeType.COMMIT)
      - FlushModeType.AUTO : 기본값, 커밋이나 쿼리를 실행할 때 플러시
      - FlushModeType.COMMIT : 커밋할 때만 플러시
  - 영속성 컨텍스트를 비우지 않음
  - 영속성 컨텍스트의 변경내용을 데이터베이스에 동기화
  
- 준영속 상태
  - 영속 상태의 엔티티가 영속성 컨텍스트에서 분리(detached)
  - 영속성 컨텍스트가 제공하는 기능을 사용 못함
  - 만드는 방법
    - em.detach(entity) : 특정 엔티티만 준영속 상태로 전환
    - em.clear : 영속성 컨텍스트를 완전히 초기화
    - em.close : 영속성 컨텍스트 종료

### 엔티티 매핑
- 객체와 테이블 매핑: @Entity, @Table
- 필드와 컬럼 매핑: @Column
- 기본키 매핑: @Id
- 연관관계 매핑: @ManyToOne, @JoinColumn

##### 필드와 컬럼 매핑

- @Entity
  - 어노테이션이 붙은 객체는 JPA가 관리
  - 기본생성자 필수(public or protected)
  - final 클래스, enum, interface, inner 클래스 사용 안됨
  - 저장할 필드에 final 사용 안됨
  - 속성: name
- @Table
  - 엔티티와 매핑할 테이블 지정
  - 속성: name, catalog, scherma, uniqueConstraints
<br>
- 데이터베이스 스키마 자동 생성
  - DDL을 애플리케이션 실행 시점에 자동 생성
  - 데이터베이스 방언을 활용해서 데이터베이스에 맞는 적절한 DDL 생성
  - 운영에서는 사용 조심
  - 제약조건 추가 가능

```
<property name="hibernate.hbm2ddl.auto" value="create" />
```
|   옵션   | 설명                          |
|:------:|:----------------------------|
| create | 기존 테이블 삭제 후 다시 생성           |
| create-drop | create 와 같지만 종료시점에 테이블 DROP | 
| update | 변경분만 반영                     |
| validate | 엔티티와 테이블이 정상 매핑 되었는지만 확인    |
| none | 사용하지 않음                     |

##### 매핑 어노테이션 정리
| 어노테이션 | 정리 |
|:----:|:----:|
| @Column | 컬럼 매핑 |
| @Temporal | 날짜 타입 매핑 |
| @Enumerated | enum 타입 매핑 |
| @Lob | BLOB, CLOB 매핑 |
| @ Transient | 특정 필드를 컬럼에 매핑 안함 |

- @Column

|  속성  | 설명                                                                                                                                                           | 기본값 |
|:----:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----:|
| name | 필드와 매핑할 테이블의 컬럼 이름                                                                                                                                           | 객체의 필드 이름 |
| insertable, updatable| 등록, 변경 가능 여부                                                                                                                                                 | TRUE |
| nullable(DDL) | null 값의 허용 여부를 설정한다. false로 설정하면 DDL 생성 시에 not null 제약조건이 붙는다.                                                                                               | |
| unique(DDL) | @Table의 uniqueConstraints와 같지만 한 컬럼에 간단히 유니크 제약조건을 걸 때 사용한다.                                                                                                  | |
| columnDefinition(DDL) | 데이터베이스 컬럼 정보를 직접 줄 수 있다.                                                                                                                                     | 필드의 자바 타입과 방언 정보를 사용 |
| length(DDL) | 문자 길이 제약조건, String 타입에만 사용한다.                                                                                                                                | 255 |
| precision, scale(DDL) | BigDecimal 타입에서 사용한다(BigInteger도 사용할 수 있다). precision은 소수점을 포함한 전체 자 릿수를, scale은 소수의 자릿수다. 참고로 double, float 타입에는 적용되지 않는다. 아주 큰 숫자나 정밀한 소수를 다루어야 할 때만 사용한다. | precision=19, scale=2 |

- @Enumerated

|  속성  | 설명                                                                              | 기본값 |
|:----:|:--------------------------------------------------------------------------------|:-----:|
| value | EnumType.ORDINAL: enum 순서를 데이터베이스에 저장 <br> EnumType.STRING: enum 이름을 데이터베이스에 저장 | EnumType.ORDINAL |

- @Temporal <br>
날짜 타입(java.util.Date, java.util.Calendar)을 매핑할 때 사용

|  속성  | 설명                                                                              | 기본값 |
|:----:|:--------------------------------------------------------------------------------|:-----:|
| TemporalType.DATE: 날짜, 데이터베이스 date 타입과 매핑 <br> (예: 2013–10–11) <br> TemporalType.TIME: 시간, 데이터베이스 time 타입과 매핑 <br> (예: 11:11:11) <br> TemporalType.TIMESTAMP: 날짜와 시간, 데이터베이스 <br> timestamp 타입과 매핑(예: 2013–10–11 11:11:11) | |

- @Lob <br>
  - 데이터베이스 BLOB, CLOB 타입과 매핑
  - 속성이 없음

- @Transient
  - 필드 매핑 안할 때 사용
  - 주로 메모리상에서만 임시로 어떤 값을 보관하고 싶을 때 사용

##### 기본 키 매핑

- @Id
- @GeneratedValue 
  - IDENTITY: 데이터베이스에 위임, MYSQL, DB에 insert 해야 키값을 알 수 있음, persist 시 바로 동작
```
@Entity 
public class Member { 
 @Id 
 @GeneratedValue(strategy = GenerationType.IDENTITY) 
 private Long id; 
```
  - SEQUENCE: 데이터베이스 시퀀스 오브젝트 사용, ORACLE
    - @SequenceGenerator 필요
```
@Entity 
@SequenceGenerator( 
 name = "MEMBER_SEQ_GENERATOR", 
 sequenceName = "MEMBER_SEQ", //매핑할 데이터베이스 시퀀스 이름
 initialValue = 1, allocationSize = 1) 
public class Member { 
 @Id 
 @GeneratedValue(strategy = GenerationType.SEQUENCE, 
 generator = "MEMBER_SEQ_GENERATOR") 
 private Long id; 
```
|  속성  | 설명                                                                              |        기본값         |
|:----:|:--------------------------------------------------------------------------------|:------------------:|
| name | 식별자 생성기 이름 |         필수         |
| sequenceName | 데이터베이스에 등록되어 있는 시퀀스 이름 | hibernate_sequence |
| initialValue | DDL 생성 시에만 사용됨, 시퀀스 DDL을 생성할 때 처음 1 시작하는 수를 지정한다. |         1          |
| allocationSize | 시퀀스 한 번 호출에 증가하는 수 | 50 |
| catalog, schema | 데이터베이스 catalog, schema 이름 | |
  - TABLE: 키 생성용 테이블 사용, 모든 DB에서 사용
    - @TableGenerator 필요
```
@Entity 
@TableGenerator( 
 name = "MEMBER_SEQ_GENERATOR", 
 table = "MY_SEQUENCES", 
 pkColumnValue = "MEMBER_SEQ", allocationSize = 1) 
public class Member { 
 @Id 
 @GeneratedValue(strategy = GenerationType.TABLE, 
 generator = "MEMBER_SEQ_GENERATOR") 
 private Long id; 
```
|  속성  | 설명                                                  |        기본값         |
|:----:|:----------------------------------------------------|:------------------:|
| name | 식별자 생성기 이름                                          | 필수 |
| table|  키생성 테이블명 | hibernate_sequences |
| pkColumnName|  시퀀스 컬럼명 | sequence_name |
| valueColumnName | 시퀀스 값 컬럼명 | next_val                                  |
| pkColumnValue|  키로 사용할 값 이름 | 엔티티 이름 |
| initialValue|  초기 값, 마지막으로 생성된 값이 기준이다. | 0 |
| allocationSize|  시퀀스 한 번 호출에 증가하는 수(성능 최적화에 사용됨) | 50 |
| catalog, schema|  데이터베이스 catalog, schema 이름 |                                                     |
| uniqueConstraints(DDL) | 유니크 제약 조건을 지정할 수 있다. <br> - AUTO: 방언에 따라 자동 지정, 기본값 | |


### 연관관계 매핑 기초

- 연관관계가 필요한 이유
  - 객체를 테이블에 맞추어 데이터 중심으로 모델링하면, 협력 관계를 만들 수 없음
- 단방향 연관관계
- 양방향 연관관계와 연관관계의 주인
  - 연관관계의 주인만이 외래 키를 관리(등록, 수정)
  - 외래 키가 있는 있는 곳을 주인으로 선택
  - 주인이 아닌 곳에 mappedBy 사용
  - 주인이 아닌쪽은 읽기만 가능

### 다양한 연관관계 매핑

##### 연관관계 매핑시 고려사항 3가지
- 다중성
  - 다대일: @ManyToOne
  - 일대다: @OneToMany
  - 일대일: @OneToOne
  - 다대다: @ManyToMany
- 단방향
  - 한쪽만 참조
- 양방향
  - 참조용 필드가 있는 쪽으로만 참조 가능
  - 양쪽이 서로 참조
- 연관관계의 주인
  - 외래 키를 관리하는 참조
- @JoinColumn

|  속성  | 설명                                                  |        기본값         |
|:----:|:----------------------------------------------------|:------------------:|
| name | 매핑할 외래 키 이름 | 필드명 + _ + 참조하는 테이블의 기본 키 컬럼명 |
| referencedColumnName | 외래 키가 참조하는 대상 테이블의 컬럼명 | 참조하는 테이블의 기본키 컬럼명 |
| foreignKey(DDL) | 외래 키 제약조건을 직접 지정할 수 있다. <br> 이 속성은 테이블을 생성할 때만 사용한다. | |
| unique <br> nullable insertable <br> updatable <br> columnDefinition <br> table | @Column의 속성과 같다. | |

- @ManyToOne

|  속성  | 설명                                                  |        기본값         |
|:----:|:----------------------------------------------------|:------------------:|
| optional | false로 설정하면 연관된 엔티티가 항상 있어야 한다. | TRUE |
| fetch | 글로벌 페치 전략을 설정한다. | @ManyToOne=FetchType.EAGER <br>  @OneToMany=FetchType.LAZY |
| cascade | 영속성 전이 기능을 사용한다. | |
| targetEntity | 연관된 엔티티의 타입 정보를 설정한다. | |

- @OneToMany

|  속성  | 설명                                                  |        기본값         |
|:----:|:----------------------------------------------------|:------------------:|
| mappedBy | 연관관계의 주인 필드를 선택한다. | |
| fetch | 글로벌 페치 전략을 설정한다. | @ManyToOne=FetchType.EAGER <br>  @OneToMany=FetchType.LAZY |
| cascade | 영속성 전이 기능을 사용한다. | |
| targetEntity | 연관된 엔티티의 타입 정보를 설정한다. | |

### 고급 매핑
- 상속관계 매핑
  - 객체의 상속과 구조와 DB의 슈퍼타입 서브타입 관계를 매핑
- 주요 어노테이션
  - @Inheritance(strategy=InheritanceType.XXX)
    - JOINED: 조인 전략
    - SINGLE_TABLE: 단일 테이블 전략
    - TABLE_PER_CLASS: 구현 클래스마다 테이블 전략
  - @DiscriminatorColumn(name=“DTYPE”)
  - @DiscriminatorValue(“XXX”)

- 조인 전략(JOINED)
  - 장점
    - 테이블 정규화
    - 외래 키 참조 무결성 제약조건 활용가능
    - 저장공간 효율화
  - 단점 
    - 조회시 조인을 많이 사용, 성능 저하
    - 조회 쿼리가 복잡함
    - 데이터 저장시 INSERT SQL 2번 호출

- 단일 테이블 전략(SINGLE_TABLE)
  - 장점
    - 조인이 필요 없으므로 일반적으로 조회 성능이 빠름
    - 조회 쿼리가 단순함
  - 단점
    - 자식 엔티티가 매핑한 컬럼은 모두 null 허용
    - 단일 테이블에 모든 것을 저장하므로 테이블이 커질 수 있음
    - 상황에 따라서 조회 성능이 오히려 느려질 수 있음

- 구현 클래스마다 테이블 전략(TABLE_PER_CLASS)
  - 장점
    - 서브 타입을 명확하게 구분해서 처리할 때 효과적
    - not null 제약조건 사용 가능
  - 단점
    - 여러 자식 테이블을 함께 조회할 때 성능이 느림(UNION SQL 필요)
    - 자식 테이블을 통합해서 쿼리하기 어려움

- @MappedSuperclass
  - 공통 매핑 정보가 필요할 때 사용
  - 부모 클래스를 상속 받는 자식 클래스에 매핑 정보만 제공
  - 추상 클래스 권장

### 프록시와 연관관계 관리

##### 프록시
- em.find()
  - 데이터베이스를 통해서 실제 엔티티 객체 조회
- em.getReference()
  - 데이터베이스 조회를 미루는 가짜(프록시) 엔티티 객체 조회

- 특징
  - 프록시는 실제 클래스를 상속 받아서 만들어짐
  - 프록시 객체를 호출하면 프록시 객체는 실제 객체의 메소드 호출
  - 처음 사용할 때 한 번만 초기화, 프록시 객체가 실제 엔티티로 바뀌는 것은 아님
  - 원본 엔티티를 상속받음, 따라서 타입 체크시 주의
  - 영속성 컨텍스트에 찾는 엔티티가 이미 있으면 em.getReference()를 호출해도 실제 엔티티 반환
  - 영속성 컨텍스트의 도움을 받을 수 없는 준영속 상태일 때, 프록시를 초기화하면 에러 발생

- 프록시 확인
  - PersistenceUnitUtil.isLoaded(Object entity)
    - 프록시 인스턴스의 초기화 여부 확인
  - entity.getClass().getName()
    - 프록시 클래스 확인 방법
  - org.hibernate.Hibernate.initialize(entity)
    - 프록시 강제 초기화
    - JPA 표준은 강제 초기화 없음

##### 즉시 로딩과 지연 로딩

- 지연로딩 fetch = FetchType.LAZY
- 즉시로딩 fetch = FetchType.EAGER
- 가급적 지연 로딩만 사용
- 즉시 로딩은 JPQL에서 N+1 문제 발생
- @ManyToOne, @OneToOne은 기본이 즉시 로딩
- @OneToMany, @ManyToMany는 기본이 지연 로딩

** JPQL fetch 조인이나, 엔티티 그래프 기능, 배치사이즈를 사용하여 N+1 대처 가능(추후 재학습)

##### 영속성 전이(CASCADE)와 고아객체

- 참조하는 곳이 하나일 때 사용해야함
- 영속성 전이: CASCADE
  - 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속상태로 만들도 싶을 때
- CASCADE의 종류
  - ALL: 모두 적용
  - PERSIST: 영속
  - REMOVE: 삭제
  - MERGE: 병합
  - REFRESH: REFRESH
  - DETACH: DETACH
```
@OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
```

- 고아객체
  - 고아 객체 제거: 부모 엔티티와 연관관계가 끊어진 자식 엔티티를 자동으로 삭제
  - 참조가 제거된 엔티티는 다른 곳에서 참조하지 않는 고아 객체로 보고 삭제하는 기능
  - CascadeType.REMOVE처럼 동작
```
orphanRemoval = true
```

### 값 타입

##### 기본값 타입
- 엔티티 타입
  - @Entity로 정의하는 객체
  - 데이터가 변해도 식별자로 지속해서 추적 가능
- 값 타입
  - int, Integer, String처럼 단순히 값으로 사용하는 자바 기본 타입이나 객체
  - 식별자가 없고 값만 있으므로 변경시 추적 불가
- 값 타입 분류
  - 기본값 타입
    - 자바 기본 타입(int, double)
    - 래퍼 클래스(Integer, Long)
    - String
  - 임베디드 타입
  - 컬렉션 값 타입
- 기본값 타입
  - 생명주기를 엔티티의 의존
  - 기본 타입은 항상 값을 복사함

##### 임베디드 타입
- 새로운 값 타입을 직접 정의할 수 있음
- 주로 기본 값 타입을 모아서 만들어서 복합 값 타입이라고도 함
- @Embeddable: 값 타입을 정의하는 곳에 표시
- @Embedded: 값 타입을 사용하는 곳에 표시
- 기본 생성자 필수
- 장점
  - 재사용
  - 높은 응집도
  - 의미 있는 메소드 만들수 있음
  - 소유한 엔티티에 생명주기를 의존함
- @AttributeOverride
  - 한 엔티티에서 같은 값 타입을 사용
  - @AttributeOverrides, @AttributeOverride를 사용해서 컬러 명 속성을 재정의
- 임베디드 타입의 값이 null이면 매핑한 컬럼 값은 모두 null

##### 값 타입과 불변객체
- 값 타입 공유 참조
  - 임베디드 타입 같은 값 타입을 여러 엔티티에서 공유하면 위험
- 불변 객체
  - 생성 시점 이후 절대 값을 변경할 수 없는 객체
  - 값 타입은 불변 객체(immutable object)로 설계해야함
  - 값 타입은 불변 객체(immutable object)로 설계해야함

##### 값 타입의 비교
- 동일성(identity) 비교: 인스턴스의 참조 값을 비교, == 사용
- 동등성(equivalence) 비교: 인스턴스의 값을 비교, equals() 사용
- 값 타입은 a.equals(b)를 사용해서 동등성 비교를 해야 함

##### 값 타입 컬렉션
- 값 타입을 하나 이상 저장할 때 사용
- @ElementCollection, @CollectionTable 사용
- 컬렉션을 저장하기 위한 별도의 테이블이 필요함
- 값 타입 컬렉션은 영속성 전이(Cascade) + 고아 객체 제거 기능을 필수로 가진다고 볼 수 있음
- 값 타입 컬렉션의 제약사항
  - 식별자 개념이 없어 값 변경 시 추적이 어려움
  - 값 타입 컬렉션에 변경 사항이 발생하면, 주인 엔티티와 연관된 모든 데이터를 삭제하고, 값 타입 컬렉션에 있는 현재 값을 모두 다시 저장
- 값 타입 컬렉션을 매핑하는 테이블은 모든 컬럼을 묶어서 기본키를 구성해야 함
- 값 타입 컬렉션 대신에 일대다 관계를 고려함!

### 객체지향 쿼리 언어

##### 객체지향 쿼리 언어 소개
- JPQL
  - 가장 단순한 조회 방법
  - 엔티티 객체를 중심으로 개발
  - 검색을 할 때도 테이블이 아닌 엔티티 객체를 대상으로 검색, 모든 DB 데이터를 객체로 변환해서 검색하는 것은 불가능 -> 객체 지향 쿼리 언어 제공
- JPA Criteria
  - 문자가 아닌 자바코드로 JPQL을 작성할 수 있음
  - JPQL 빌더 역할
  - JPA 공식 기능
  - 너무 복잡하고 실용성이 없음!
  - 대신 QueryDSL 사용 권장
- QueryDSL
  - 문자가 아닌 자바코드로 JPQL을 작성할 수 있음
  - JPQL 빌더 역할
  - 컴파일 시점에 문법 오류를 찾을 수 있음
  - 동적쿼리 작성 편리함
  - 단순하고 쉬움
  - 실무 사용 권장!
- 네이티브 SQL
  - SQL을 직접 사용
  - JPQL로 해결할 수 없는 특정 데이터베이스에 의존적인 기능 사용시
- JDBC API 직접 사용, MyBatis, SpringJdbcTemplate 함께 사용
  - 단 영속성 컨텍스트를 적절한 시점에 강제로 플러시 필요

##### JPQL
- 엔티티 객체를 대상으로 쿼리
- SQL을 추상화해서 특정데이터베이스 SQL에 의존하지 않음
- 엔티티와 속성은 대소문자 구분
- JPQL 키워드는 대소문자 구분 안함
- 엔티티 이름 사용, 테이블 이름이 아님
- 별칭 필수
- 집합과 정렬
  - COUNT
  - SUM
  - AVG
  - MAX
  - MIN
  - GROUP BY, HAVING, ORDER BY
- TypeQuery: 반환 타입이 명확할 때 사용
- Query: 반환 타입이 명확하지 않을 때 사용
```
TypedQuery<Member> query = em.createQuery("SELECT m FROM Member m", Member.class); 
Query query = em.createQuery("SELECT m.username, m.age from Member m");
```
- query.getResultList(): 결과가 하나 이상일 때
- query.getSingleResult(): 결과가 정확히 하나일 때
  - 결과가 없으면: javax.persistence.NoResultException
  - 둘 이상이면: javax.persistence.NonUniqueResultException
- 파라미터 바인딩
  - 이름 기준
  - 위치 기준
```
SELECT m FROM Member m where m.username=:username
query.setParameter("username", usernameParam);

SELECT m FROM Member m where m.username=?1
query.setParameter(1, usernameParam);
```

##### 프로젝션
- SELECT 절에 조회할 대상을 지정하는 것
- 대상: 엔티티, 임베디드 타입, 스칼라 타입
- DISTINCT로 중복 제거 가능
- 여러 값 조회
  1. Query 타입으로 조회
  2. Object[] 타입으로 조회
  3. new 명령어로 조회
     - 패키지 명을 포함한 전체 클래스 명 입력
     - 순서와 타입이 일치하는 생성자 필요

```
// 1.
List resultList = em.createQuery("select m.username, m.age from Member as m").getResultList();
Object o = resultList.get(0);
Object[] result = (Object[]) o;
System.out.println("result = " + result.get(0));
System.out.println("result = " + result.get(1));

// 2.
List<Object[]> resultList = em.createQuery("select m.username, m.age from Member as m").getResultList();
System.out.println("result = " + resultList.get(0));
System.out.println("result = " + resultList.get(1));

// 3.
SELECT new jpabook.jpql.UserDTO(m.username, m.age) FROM Member m
```

##### 페이징
- setFirstResult(int startPosition) : 조회 시작 위치
- setMaxResults(int maxResult) : 조회할 데이터 수
```
String jpql = "select m from Member m order by m.name desc";
List<Member> resultList = em.createQuery(jpql, Member.class)
                            .setFirstResult(10)
                            .setMaxResults(20)
                            .getResultList();
```

##### 조인
- 내부 조인
  - SELECT m FROM Member m [INNER] JOIN m.team t
- 외부 조인
  - SELECT m FROM Member m LEFT [OUTER] JOIN m.team t
- 세타 조인
  - select count(m) from Member m, Team t where m.username = t.name
- ON절을 활용한 조인
  - 조인 대상 필터링
    - SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'A'
  - 연관관계 없는 엔티티 외부 조인
    - SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name

##### 서브 쿼리
- [NOT] EXISTS (subquery): 서브쿼리에 결과가 존재하면 참
  - ALL 모두 만족하면 참
  - ANY, SOME: 같은 의미, 조건을 하나라도 만족하면 참
- [NOT] IN (subquery): 서브쿼리의 결과 중 하나라도 같은 것이 있으면 참
- 한계
  - WHERE, HAVING 절에서만 서브 쿼리 사용 가능(JPA)
  - SELECT 절도 가능(하이버네이트)
  - FROM 절의 서브 쿼리는 현재 JPQL에서 불가능 -> 조인으로 풀어서 해결

##### JPQL 타입 표현과 기타식
- 문자: ‘HELLO’, ‘She’’s’
- 숫자: 10L(Long), 10D(Double), 10F(Float)
- Boolean: TRUE, FALSE
- ENUM: jpabook.MemberType.Admin (패키지명 포함)
- 엔티티 타입: TYPE(m) = Member (상속 관계에서 사용)
- SQL과 문법이 같은 식
  - EXISTS, IN
  - AND, OR, NOT
  - =, >, >=, <, <=, <>
  - BETWEEN, LIKE, IS NULL

##### 조건식
- 기본 CASE 식
```
select
 case when m.age <= 10 then '학생요금'
 when m.age >= 60 then '경로요금'
 else '일반요금'
 end
from Member m
```
- 단순 CASE 식
```
select
 case t.name 
 when '팀A' then '인센티브110%'
 when '팀B' then '인센티브120%'
 else '인센티브105%'
 end
from Team t
```
- COALESCE: 하나씩 조회해서 null이 아니면 반환
- NULLIF: 두 값이 같으면 null 반환, 다르면 첫번째 값 반환

##### JPQL 함수
- CONCAT
- SUBSTRING
- TRIM
- LOWER, UPPER
- LENGTH
- LOCATE
- ABS, SQRT, MOD
- SIZE, INDEX(JPA 용도)
- 사용자 정의 함수 호출
  - 사용전 방언에 추가해야 함
  - 사용하는 DB 방언을 상속받고, 사용자 정의 함수를 등록

##### 경로 표현식
- .(점)을 찍어 객체 그래프를 탐색하는 것
- 상태 필드(state field): 단순히 값을 저장하기 위한 필드
- 연관 필드(association field): 연관관계를 위한 필드
  - 단일 값 연관 필드: @ManyToOne, @OneToOne, 대상이 엔티티(ex: m.team)
  - 컬렉션 값 연관 필드: @OneToMany, @ManyToMany, 대상이 컬렉션(ex: m.orders)
- 특징
  - 상태 필드(state field): 경로 탐색의 끝, 탐색 불가
  - 단일 값 연관 경로: 묵시적 내부 조인(inner join) 발생, 탐색 가능
  - 컬렉션 값 연관 경로: 묵시적 내부 조인 발생, 탐색 불가
    - FROM 절에서 명시적 조인을 통해 별칭을 얻으면 별칭을 통해 탐색 가능
- 명시적 조인: join 키워드 직접 사용
- 묵시적 조인: 경로 표현식에 의해 묵시적으로 SQL 조인 발생(내부조인만 가능)
- 가급적 묵시적 조인 대신에 명시적 조인 사용!

##### 페치 조인
- JPQL에서 성능 최적화를 위해 제공하는 기능
- 연관된 엔티티나 컬렉션을 SQL 한 번에 함께 조회하는 기능
- 지연로딩 설정보다 우선순위가 높음
- JPQL의 DISTINCT : DISTINCT는 중복된 결과를 제거하는 명령
  - SQL에 DISTINCT를 추가
  - 애플리케이션에서 엔티티 중복 제거
- 일반조인과 비교
  - 일반조인 : SELECT 절에 지정한 엔티티만 조회
  - 페치조인 : 연관된 엔티티나 컬렉션 모두 조회(즉시 로딩), 페치 조인은 객체 그래프를 SQL 한번에 조회하는 개념

##### 페치 조인 한계
- 페치 조인 대상에는 별칭을 줄 수 없음(하이버네이트는 가능하지만 가급적 사용 비추천)
- 둘 이상의 컬렉션은 페치 조인 할 수 없음
- 컬렉션을 페치 조인하면 페이징 API(setFirstResult, setMaxResults)를 사용할 수 없음(하이버네이트는 경고 로그를 남기고 메모리에서 페이징)
- @BatchSize(size=값) : LAZY 로딩된 컬랙션을 IN 쿼리로 한번에 조회되도록 하는 어노테이션
- 여러 테이블을 조인해서 엔티티가 가진 모양이 아닌 전혀 다른 결과를 내야 하면, 페치 조인 보다는 일반 조인을 사용하고 필요한 데이터들만 조회해서 DTO로 반환하는 것이 효과적

##### 다형성 쿼리
- TYPE
  - 조회 대상을 특정 자식으로 한정
```
select i from Item i
where type(i) IN (Book, Movie)
```
- TREAT
  - 자바의 타입 캐스팅과 유사
  - 상속 구조에서 부모 타입을 특정 자식 타입으로 다룰 때 사용
  - FROM, WHERE, SELECT에서 사용
```
select i from Item i
where treat(i as Book).auther = ‘kim’
```

##### 엔티티 직접 사용












