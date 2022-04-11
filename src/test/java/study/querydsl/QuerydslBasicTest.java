package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  private EntityManager em;

  @BeforeEach
  public void before() {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  void startJPQL() {
    //find member1
    Member result = em.createQuery("select m from Member m where m.username = :username",
            Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(result.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQueryDsl() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    QMember m = new QMember("m");

    Member result = jpaQueryFactory
        .select(m)
        .from(m)
        .where(m.username.eq("member1"))
        .fetchOne();

    assertThat(result.getUsername()).isEqualTo("member1");
  }

  @Test
  void search() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    Member member1 = jpaQueryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(member1.getUsername()).isEqualTo("member1");
  }

  @Test
  void searchAndParam() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    Member member1 = jpaQueryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            , member.age.eq(10))
        .fetchOne();

    assertThat(member1.getUsername()).isEqualTo("member1");
  }

  @Test
  void resultFetchTest() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Member> fetch = jpaQueryFactory
        .selectFrom(member)
        .fetch();

    Member fetchOne = jpaQueryFactory
        .selectFrom(QMember.member)
        .fetchOne();

    Member fetchFirst = jpaQueryFactory
        .selectFrom(QMember.member)
        .fetchFirst();

    QueryResults<Member> results = jpaQueryFactory
        .selectFrom(member)
        .fetchResults();
    
    results.getTotal();
    List<Member> contents = results.getResults();
  }

  @Test
  void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    List<Member> fetch = jpaQueryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    Member member5 = fetch.get(0);
    Member member6 = fetch.get(1);
    Member memberNull = fetch.get(2);
    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();

  }

  @Test
  void paging() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Member> result = jpaQueryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void aggregation() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Tuple> result = jpaQueryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min()
        )
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
  }

  @Test
  void group() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Tuple> result = jpaQueryFactory
        .select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
  }

  @Test
  void join() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Member> result = jpaQueryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();
  }

  @Test
  void join_on_filtering() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    List<Tuple> result = jpaQueryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team).on(team.name.eq("teamA"))
        .fetch();
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  void fetchJoinNo() {

    em.flush();
    em.clear();

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    Member one = jpaQueryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(one.getTeam());
    assertThat(loaded).isFalse();
  }

  @Test
  void fetchJoinUse() {

    em.flush();
    em.clear();

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    Member one = jpaQueryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(one.getTeam());
    assertThat(loaded).isTrue();
  }

  @Test
  void subQuery() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    QMember memberSub = new QMember("memberSub");

    List<Member> fetch = jpaQueryFactory
        .selectFrom(member)
        .where(member.age.eq(
            JPAExpressions.select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();

    assertThat(fetch).extracting("age")
        .containsExactly(40);
  }

  @Test
  void subQuery2() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    QMember memberSub = new QMember("memberSub");

    List<Member> fetch = jpaQueryFactory
        .selectFrom(member)
        .where(member.age.goe(
            JPAExpressions.select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(fetch).extracting("age")
        .containsExactly(30, 40);
  }

  @Test
  void basicCase() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    jpaQueryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("이십살")
            .otherwise("기타"))
        .from(member)
        .fetch();
  }

  @Test
  void constant() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    jpaQueryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();
  }

  @Test
  void concat() {
    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

    jpaQueryFactory
        .select(member.username.concat("_").concat(String.valueOf(member.age)))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();
  }


}
