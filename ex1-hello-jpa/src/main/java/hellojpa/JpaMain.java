package hellojpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.List;

public class JpaMain {
    public static void main(String[] args) {
        // persistence.xml에 설정된 persistence-unit의 name으로 EntityManagerFactory 설정
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
            // Insert 방법
//            Member member = new Member();
//
//            member.setId(2L);
//            member.setName("HelloB");
//
//            em.persist(member);

            // Select
//            Member findMember = em.find(Member.class, 1L);
//            System.out.println("findMember.id = " + findMember.getId());
//            System.out.println("findMember.name = " + findMember.getName());

            // Update
//            findMember.setName("HelloJPA");

            // JPQL
//            List<Member> result = em.createQuery("select m from Member as m", Member.class)
//                    .setFirstResult(0)
//                    .setMaxResults(1)
//                    .getResultList();
//            for (Member member : result) {
//                System.out.println("member.name = " + member.getName());
//            }

//            Member member = new Member();
//
//            member.setId(101L);
//            member.setName("HelloJPA");
//
//            System.out.println("======AFTER======");
//            em.persist(member);
//            System.out.println("======BEFORE======");
//
//            Member findMember = em.find(Member.class, 101L);
//            System.out.println("findMember.id = " + findMember.getId());
//            System.out.println("findMember.name = " + findMember.getName());

//            Member findMember1 = em.find(Member.class, 101L);
//            Member findMember2 = em.find(Member.class, 101L);
//
//            System.out.println("result = " + (findMember1 == findMember2));

//            Member member1 = new Member(150L, "A");
//            Member member2 = new Member(160L, "B");
//
//            em.persist(member1);
//            em.persist(member2);
//            System.out.println("================");

//            Member member = em.find(Member.class, 150L);
//            member.setName("ZZZZZ");
//            System.out.println("================");

            Member member = new Member(200L, "member200");
            em.persist(member);

            em.flush();

            System.out.println("================");
            tx.commit();
        } catch (Exception ex) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }
}
