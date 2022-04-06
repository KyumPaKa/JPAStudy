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
