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
- 객체는 참조를 사용, DB는 외래키를 사용
- SQL에 따라 탐색 범위 결정, 객체는 자유롭게 객체 그래프를 탐색 가능해야함
- 엔티티 신뢰 문제 -> 상황에 따른 모든 조회 메서드 생성해야함
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
