# 실전! 스프링 부트와 JPA 활용2 - API 개발과 성능 최적화

### API 개발 고급 - 지연 로딩과 조회 성능 최적화

##### V1: 엔티티를 직접 노출
```
@GetMapping("/api/v1/simple-orders")
public List<Order> orders() {
    List<Order> all = orderRepository.findAllByCriteria(new OrderSearch());
    for (Order order : all) {
        order.getMember().getName(); // Lazy 강제 초기화
        order.getDelivery().getAddress(); // Lazy 강제 초기화
    }
    return all;
}
```
##### V2: 엔티티를 DTO로 변환
```
@GetMapping("/api/v2/simple-orders")
public List<SimpleOrderDto> orderV2() {
    return orderRepository.findAllByCriteria(new OrderSearch()).stream()
            .map(SimpleOrderDto::new)
            .collect(Collectors.toList());
}
```
##### V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
```
@GetMapping("/api/v3/simple-orders")
public List<SimpleOrderDto> ordersV3() {
 List<Order> orders = orderRepository.findAllWithMemberDelivery();
 List<SimpleOrderDto> result = orders.stream()
 .map(o -> new SimpleOrderDto(o))
 .collect(toList());
 return result;
}

public List<Order> findAllWithMemberDelivery() {
 return em.createQuery(
 "select o from Order o" +
 " join fetch o.member m" +
 " join fetch o.delivery d", Order.class)
 .getResultList();
}
```
##### V4: JPA에서 DTO로 바로 조회
```
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {
 private final EntityManager em;
 public List<OrderSimpleQueryDto> findOrderDtos() {
 return em.createQuery(
 "select new 
jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, 
o.orderDate, o.status, d.address)" +
 " from Order o" +
 " join o.member m" +
 " join o.delivery d", OrderSimpleQueryDto.class)
 .getResultList();
 }
}
```

### API 개발 고급 - 컬렉션 조회 최적화

##### V1: 엔티티 직접 노출
```
@GetMapping("/api/v1/orders")
public List<Order> ordersV1() {
    List<Order> all = orderRepository.findAll();
    for (Order order : all) {
        order.getMember().getName(); //Lazy 강제 초기화
        order.getDelivery().getAddress(); //Lazy 강제 초기환
        List<OrderItem> orderItems = order.getOrderItems();
        orderItems.stream().forEach(o -> o.getItem().getName()); //Lazy 강제 초기화
    }
    return all;
}
```
##### V2: 엔티티를 DTO로 변환
```
@GetMapping("/api/v2/orders")
public List<OrderDto> ordersV2() {
    List<Order> orders = orderRepository.findAll();
    List<OrderDto> result = orders.stream()
    .map(o -> new OrderDto(o))
    .collect(toList());
    return result;
}
```
##### V3: 엔티티를 DTO로 변환 - 페치 조인 최적화
```
@GetMapping("/api/v3/orders")
public List<OrderDto> ordersV3() {
    List<Order> orders = orderRepository.findAllWithItem();
    List<OrderDto> result = orders.stream()
    .map(o -> new OrderDto(o))
    .collect(toList());
    return result;
}

public List<Order> findAllWithItem() {
    return em.createQuery(
            "select distinct o from Order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d" +
            " join fetch o.orderItems oi" +
            " join fetch oi.item i", Order.class)
            .getResultList();
}
```
##### V3.1: 엔티티를 DTO로 변환 - 페이징과 한계 돌파
- 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size , @BatchSize 를 적용
```
public List<Order> findAllWithMemberDelivery(int offset, int limit) {
    return em.createQuery(
            "select o from Order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d", Order.class)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
}

spring:
    jpa:
        properties:
            hibernate:
                default_batch_fetch_size: 1000
```
##### V4: JPA에서 DTO 직접 조회
```
public List<OrderQueryDto> findOrderQueryDtos() {
    //루트 조회(toOne 코드를 모두 한번에 조회)
    List<OrderQueryDto> result = findOrders();
    //루프를 돌면서 컬렉션 추가(추가 쿼리 실행)
    result.forEach(o -> {
        List<OrderItemQueryDto> orderItems =
        findOrderItems(o.getOrderId());
        o.setOrderItems(orderItems);
    });
    return result;
}

private List<OrderQueryDto> findOrders() {
    return em.createQuery(
            "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
            " from Order o" +
            " join o.member m" +
            " join o.delivery d", OrderQueryDto.class)
            .getResultList();
}
 
private List<OrderItemQueryDto> findOrderItems(Long orderId) {
    return em.createQuery(
            "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
            " from OrderItem oi" +
            " join oi.item i" +
            " where oi.order.id = : orderId", OrderItemQueryDto.class)
            .setParameter("orderId", orderId)
            .getResultList();
}
```
#####  V5: JPA에서 DTO 직접 조회 - 컬렉션 조회 최적화
```
public List<OrderQueryDto> findAllByDto_optimization() {
    //루트 조회(toOne 코드를 모두 한번에 조회)
    List<OrderQueryDto> result = findOrders();
    //orderItem 컬렉션을 MAP 한방에 조회
    Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));
    //루프를 돌면서 컬렉션 추가(추가 쿼리 실행X)
    result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
    return result;
}

private List<Long> toOrderIds(List<OrderQueryDto> result) {
    return result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());
}

private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long>
orderIds) {
    List<OrderItemQueryDto> orderItems = em.createQuery("select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
    " from OrderItem oi" +
    " join oi.item i" +
    " where oi.order.id in :orderIds", OrderItemQueryDto.class)
    .setParameter("orderIds", orderIds)
    .getResultList();
    return orderItems.stream()
                    .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
}
```
##### V6: JPA에서 DTO로 직접 조회, 플랫 데이터 최적화
```
@GetMapping("/api/v6/orders")
public List<OrderQueryDto> ordersV6() {
    List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
    return flats.stream().collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
            o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
            mapping(o -> new OrderItemQueryDto(o.getOrderId(),
            o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
            )).entrySet().stream()
            .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
            e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
            e.getKey().getAddress(), e.getValue()))
            .collect(toList());
}
```

### OSIV와 성능 최적화
- Open Session In View: 하이버네이트
- Open EntityManager In View: JPA

- spring.jpa.open-in-view : true 기본값
- OSIV 전략은 트랜잭션 시작처럼 최초 데이터베이스 커넥션 시작 시점부터 API 응답이 끝날 때 까지 영속성 컨텍스트와 데이터베이스 커넥션을 유지









