import jpql.Member;
import jpql.Team;

import javax.persistence.*;
import java.util.Collection;
import java.util.List;

public class JpaMain {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
            Team team1 = new Team();
            team1.setName("team1");
            em.persist(team1);

            Team team2 = new Team();
            team2.setName("team2");
            em.persist(team2);

            Member member1 = new Member();
            member1.setUsername("username1");
            member1.setAge(10);
            member1.addTeam(team1);
            em.persist(member1);

            Member member2 = new Member();
            member2.setUsername("username2");
            member2.setAge(10);
            member2.addTeam(team1);
            em.persist(member2);

            Member member3 = new Member();
            member3.setUsername("username3");
            member3.setAge(10);
            member3.addTeam(team2);
            em.persist(member3);

            Member member4 = new Member();
            member4.setUsername("username4");
            member4.setAge(10);
            em.persist(member4);

            em.flush();
            em.clear();

//            String query = "select m from Member as m join fetch m.team";
//            String query = "select distinct t from Team as t join fetch t.members";
            String query = "select t from Team as t join t.members";

            List<Team> resultList = em.createQuery(query, Team.class)
                    .getResultList();

//            for (Member member : resultList) {
//                System.out.println("member = " + member.getUsername() + ", team = " + member.getTeam().getName());
//            }

            for (Team team : resultList) {
                System.out.println("team = " + team.getName() + "| member = " + team.getMembers().size());
                for (Member member : team.getMembers()) {
                    System.out.println("-> member = " + member);
                }
            }

            tx.commit();
        } catch (Exception ex) {
            tx.rollback();
            ex.printStackTrace();
        } finally {
            em.close();
        }

        emf.close();
    }
}
