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
