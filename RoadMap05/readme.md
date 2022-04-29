# 실전! Querydsl

### 기본문법

##### 기본 Q-Type 활용
- Q클래스 인스턴스를 사용
```
QMember qMember = new QMember("m"); //별칭 직접 지정, 입력된 별칭으로 JPQL에서 사용
QMember qMember = QMember.member; //기본 인스턴스 사용
```
- static import 사용시 코드가 깔끔해짐
```
import static study.querydsl.entity.QMember.*;

@Test
public void startQuerydsl3() {
    Member findMember = queryFactory.select(member)
                                    .from(member)
                                    .where(member.username.eq("member1"))
                                    .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
}
```
- application 설정파일에 추가 시 JPQL 확인가능
  - spring.jpa.properties.hibernate.use_sql_comments: true

##### 검색 조건 쿼리
- .and() , . or() 를 메서드 체인으로 연결 가능
- JPQL 제공하는 검색 조건
```
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'

member.username.isNotNull() //이름이 is not null

member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30

member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30

member.username.like("member%") //like 검색
member.username.contains("member") // like ‘%member%’ 검색
member.username.startsWith("member") //like ‘member%’ 검색
...
```
- 파라미터로 처리
  - 검색조건을 추가하면 AND 조건으로 추가
  - null 값은 무시
```
Member findMember = queryFactory.selectFrom(member)
        .where(
                member.username.eq("member1"),
                member.age.eq(10)
        )
        .fetchOne();
```

##### 결과 조회
- fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
- fetchOne() : 단 건 조회
  - 결과가 없으면 : null
  - 결과가 둘 이상이면 : `com.querydsl.core.NonUniqueResultException`
- fetchFirst() : limit(1).fetchOne()
- fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행, `@Deprecated`
- fetchCount() : count 쿼리로 변경해서 count 수 조회, `@Deprecated`

##### 정렬
- .orderBy() 매개변수에 ,로 정렬기준 나열
- desc(), asc() : 일반 정렬
- nullsLast(), nullsFirst() : null 데이터 순서

##### 페이징
- .offset() : 조회시작인덱스, 0부터 시작
- .limit() : 최대조회건수

##### 집합
- count(), sum(), avg(), max(), min().. JPQL이 제공하는 집합 함수를 제공
- groupBy(), having() 사용가능
```
List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                                  .from(member)
                                  .join(member.team, team)
                                  .groupBy(team.name)
                                  .fetch();

Tuple teamA = result.get(0);
assertThat(teamA.get(team.name)).isEqualTo("teamA");
```

##### 조인 - 기본 조인
- 기본 조인
  - 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정
  - join(조인 대상, 별칭으로 사용할 Q타입)
  - join() , innerJoin() : 내부 조인(inner join)
  - leftJoin() : left 외부 조인(left outer join)
  - rightJoin() : rigth 외부 조인(rigth outer join)
```
.join(member.team, team)
```
- 세타 조인
  - 연관관계가 없는 필드로 조인
  - from 절에 여러 엔티티를 선택해서 세타 조인
  - 외부 조인 불가능했으나 최근 querydsl에서는 조인 on 으로 해결
```
.from(member, team)
```

##### 조인 - on절
- on절의 기능
  - 조인 대상 필터링
  - 연관관계 없는 엔티티 외부 조인

- 조인 대상 필터링
  - 내부조인(inner join)을 사용하면 where 절에서 필터링 하는 것과 기능이 동일

- 연관관계 없는 엔티티 외부 조인
  - 하이버네이트 5.1부터 기능 추가
  - on조인: from(member).leftJoin(team)
  - 일반조인: leftJoin(member.team, team)

##### 조인 - 페치 조인
- .fetchJoin() 추가
```
Member findMember = queryFactory.selectFrom(member)
                                .join(member.team, team).fetchJoin()
                                .where(member.username.eq("memeber1"))
                                .fetchOne();
```

##### 서브쿼리
- `com.querydsl.jpa.JPAExpressions` 사용
- select절, where절에 사용 가능하지만 from절에는 사용 불가능
- from 절의 서브쿼리 해결방안
  - 가능하면 서브쿼리를 join으로 변경
  - 애플리케이션에서 쿼리를 2번 분리해서 실행
  - nativeSQL을 사용

##### Case 문
- select, 조건절(where), order by에서 사용 가능
- 간단한 Case 문
```
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
```
- 복잡한 Case 문
```
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
```
- Order by Case 문
```
NumberExpression<Integer> rankPath = new CaseBuilder().when(member.age.between(0, 20)).then(2)
                                                      .when(member.age.between(21, 30)).then(1)
                                                      .otherwise(3);
List<Tuple> result = queryFactory.select(member.username, member.age, rankPath)
                                 .from(member)
                                 .orderBy(rankPath.desc())
                                 .fetch();
```

##### 상수, 문자 더하기
- 상수
  - Expressions.constant("A")
```
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
```
- 문자 더하기
  - concat() 사용, 문자열만 가능하므로 stringValue()로 문자 변환해야함
  - stringValue()은 ENUM Type 처리 시 필요
```
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
```

### 중급 문법

##### 프로젝션과 결과 반환 - 기본
- 프로젝션 대상이 하나면 타입 지정 가능
- 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
- `com.querydsl.core.Tuple`
```
// 대상이 하나 조회
List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

// 대상이 둘 이상 조회
List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();
// Tuple 출력 방법
for (Tuple tuple : result) {
    String username = tuple.get(member.username);
    Integer age = tuple.get(member.age);
    System.out.println("username = " + username);
    System.out.println("age = " + age);
}
```

##### 프로젝션과 결과 반환 - DTO 조회
- Querydsl 빈 생성 3가지 방법
  - 프로퍼티 접근
  - 필드 접근
  - 생성자 사용

- 프로퍼티 접근
```
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```
- 필드 접근
```
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```
- 생성자 사용
```
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
```
- 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
  - `ExpressionUtils.as(source,alias)` : 필드나, 서브 쿼리에 별칭 적용
  - `username.as("memberName")` `: 필드에 별칭 적용

##### 프로젝션과 결과 반환 - @QueryProjection
- 객체 생성자에 @QueryProjection 사용후 Q파일 생성
```
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
```

##### 동적 쿼리 - BooleanBuilder 사용
```
BooleanBuilder builder = new BooleanBuilder();
if (usernameCond != null) {
    builder.and(member.username.eq(usernameCond));
}

if (ageCond != null) {
    builder.and(member.age.eq(ageCond));
}

queryFactory.selectFrom(member)
            .where(builder)
            .fetch();
```

##### 동적 쿼리 - Where 다중 파라미터 사용
- where 조건에 null 값은 무시
- 다른 쿼리에서도 재활용 가능
- 쿼리 자체의 가독성 상승
```
queryFactory.selectFrom(member)
            .where(usernameEq(usernameCond), ageEq(ageCond))
            .fetch();
```

##### 수정, 삭제 벌크 연산
- 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전함
```
// 수정
queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .execute();

// 삭제
queryFactory
        .delete(member)
        .where(member.age.gt(18))
        .execute();
```

##### SQL function 호출하기
- Dialect에 등록된 내용만 호출 가능
- Expressions.stringTemplate() 함수 사용
```
queryFactory.select(Expressions.stringTemplate(
                    "function('replace', {0}, {1}, {2})",
                    member.username, "member", "m"))
            .from(member)
            .fetch();
```
- ansi 표준 함수들은 querydsl이 상당부분 내장하고 있음

##### 실무 활용 - 스프링 데이터 JPA와 Querydsl
- Page 인터페이스 구현체 : PageImpl
```
PageImpl(List<T> content, Pageable pageable, long total)
```
- 페이징 최적화
  - 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
  - 마지막 페이지 일 때
```
//getPage(List<T> content, Pageable pageable, LongSupplier totalSupplier)

PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
```
- 스프링 데이터 정렬
  - OrderSpecifier 사용
  - 복잡한 경우 Pageable의 Sort 기능 사용이 어려워져서 파라미터를 받아서 직접 처리하는 것을 권장
```
JPAQuery<Member> query = queryFactory.selectFrom(member);

for (Sort.Order o : pageable.getSort()) {
  PathBuilder pathBuilder = new PathBuilder(member.getType(), member.getMetadata());
  query.orderBy(new OrderSpecifier(
                        o.isAscending() ? Order.ASC : Order.DESC, 
                        pathBuilder.get(o.getProperty())
                                  )
                );
}
List<Member> result = query.fetch();
```

### 스프링 데이터 JPA가 제공하는 Querydsl 기능

##### 인터페이스 지원 - QuerydslPredicateExecutor
- QuerydslPredicateExecutor<T> 인터페이스 상속
```
memberRepository.findAll(
    member.age.between(10, 40).and(member.username.eq("member1")
));
```
- 한계점
  - left join 불가능함
  - 서비스 클래스가 Querydsl에 의존해야함

##### Querydsl Web 지원
- 단순한 조건만 가능
- 조건을 커스텀하는 기능이 복잡하고 명시적이지 않음
- 컨트롤러가 Querydsl에 의존함
- https://docs.spring.io/spring-data/jpa/docs/2.2.3.RELEASE/reference/html/#core.web.type-safe

##### 리포지토리 지원 - QuerydslRepositorySupport
- QuerydslRepositorySupport 상속
- 장점
  - getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환 가능(단, sort 오류 발생)
  - EntityManager 제공
- 단점
  - from() 으로 시작
  - JPAQueryFactory로 시작할 수 없음
  - 스프링 데이터 Sort 기능이 정상 동작하지 않음

##### Querydsl 지원 클래스 직접 만들기
- 장점
  - 스프링 데이터가 제공하는 페이징을 편리하게 변환
  - 페이징과 카운트 쿼리 분리 가능
  - 스프링 데이터 Sort 지원
  - select() , selectFrom() 으로 시작 가능
  - EntityManager , QueryFactory 제공
