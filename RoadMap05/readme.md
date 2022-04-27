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
