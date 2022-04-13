import jpql.Member;

import javax.persistence.*;
import java.util.List;

public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
//            Member member = new Member();
//            member.setUsername("username1");
//            member.setAge(10);
//            em.persist(member);

            TypedQuery<Member> query1 = em.createQuery("select m from Member as m where m.id = '10'", Member.class);
//            TypedQuery<String> query2 = em.createQuery("select m.username from Member as m", String.class);
//            Query query3 = em.createQuery("select m.username, m.age from Member as m");

//            List<Member> resultList = query1.getResultList();
//            for( Member m: resultList ) {
//                System.out.println("member = " + m.getId());
//            }

            Member singleResult = query1.getSingleResult();
            System.out.println("singleResult = " + singleResult);

            tx.commit();
        } catch (Exception ex) {
            tx.rollback();
        } finally {
            em.close();
        }

        emf.close();
    }
}
