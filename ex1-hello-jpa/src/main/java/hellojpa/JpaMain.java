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
            List<Member> result = em.createQuery("select m from Member as m", Member.class)
                    .setFirstResult(0)
                    .setMaxResults(1)
                    .getResultList();
            for (Member member : result) {
                System.out.println("member.name = " + member.getName());
            }

            tx.commit();
        } catch (Exception ex) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }
}
