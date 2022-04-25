# 실전! 스프링 부트와 JPA 활용1 - 웹 애플리케이션 개발

### 변경 감지와 병합
- 준영속 엔티티 <br>
영속성 컨텍스트가 더는 관리하지 않는 엔티티
- 준영속 엔티티를 수정하는 2가지 방법
- 변경 감지 기능 사용
```
@Transactional
void update(Item itemParam) { //itemParam: 파리미터로 넘어온 준영속 상태의 엔티티
 Item findItem = em.find(Item.class, itemParam.getId()); //같은 엔티티를 조회한
다.
 findItem.setPrice(itemParam.getPrice()); //데이터를 수정한다.
}
```
- 병합 사용
```
@Transactional
void update(Item itemParam) { //itemParam: 파리미터로 넘어온 준영속 상태의 엔티티
 Item mergeItem = em.merge(item);
}
```
- 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 모든 속성이 변경된다. 병합시 값이 없으면 null 로 업데이트 할 위험도 있음
