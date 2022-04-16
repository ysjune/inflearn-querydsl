package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.net.ProxySelector;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  private EntityManager em;

  JPAQueryFactory queryFactory;

  @BeforeEach
  public void before() {

    queryFactory = new JPAQueryFactory(em);

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
    QMember m = new QMember("m");

    Member result = queryFactory
        .select(m)
        .from(m)
        .where(m.username.eq("member1"))
        .fetchOne();

    assertThat(result.getUsername()).isEqualTo("member1");
  }

  @Test
  void search() {

    Member member1 = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(member1.getUsername()).isEqualTo("member1");
  }

  @Test
  void searchAndParam() {

    Member member1 = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            , member.age.eq(10))
        .fetchOne();

    assertThat(member1.getUsername()).isEqualTo("member1");
  }

  @Test
  void resultFetchTest() {

    List<Member> fetch = queryFactory
        .selectFrom(member)
        .fetch();

    Member fetchOne = queryFactory
        .selectFrom(QMember.member)
        .fetchOne();

    Member fetchFirst = queryFactory
        .selectFrom(QMember.member)
        .fetchFirst();

    QueryResults<Member> results = queryFactory
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

    List<Member> fetch = queryFactory
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

    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void aggregation() {

    List<Tuple> result = queryFactory
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

    List<Tuple> result = queryFactory
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

    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();
  }

  @Test
  void join_on_filtering() {

    List<Tuple> result = queryFactory
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

    Member one = queryFactory
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

    Member one = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(one.getTeam());
    assertThat(loaded).isTrue();
  }

  @Test
  void subQuery() {

    QMember memberSub = new QMember("memberSub");

    List<Member> fetch = queryFactory
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

    QMember memberSub = new QMember("memberSub");

    List<Member> fetch = queryFactory
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

    queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("이십살")
            .otherwise("기타"))
        .from(member)
        .fetch();
  }

  @Test
  void constant() {

    queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();
  }

  @Test
  void concat() {

    queryFactory
        .select(member.username.concat("_").concat(String.valueOf(member.age)))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();
  }

  @Test
  void simpleProjection() {
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    System.out.println(result);
  }

  @Test
  void tupleProjection() {
    List<Tuple> fetch = queryFactory.select(member.username, member.age)
        .from(member)
        .fetch();

    for (Tuple tuple : fetch) {
      String s = tuple.get(member.username);
      Integer integer = tuple.get(member.age);

      System.out.println("username " + s);
      System.out.println("age" + integer);
    }

  }

  @Test
  void findDtoByJPQL() {

    List<MemberDto> resultList = em.createQuery(
            "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
        .getResultList();

    for (MemberDto memberDto : resultList) {
      System.out.println(memberDto);
    }
  }

  @Test
  void findDtoBySetter() {

    List<MemberDto> fetch = queryFactory.select(
            Projections.bean(MemberDto.class, member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : fetch) {
      System.out.println(memberDto);
    }
  }

  @Test
  void findDtoByField() {
    List<MemberDto> fetch = queryFactory.select(
            Projections.fields(MemberDto.class, member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : fetch) {
      System.out.println(memberDto);
    }
  }

  @Test
  void findDtoByConstructor() {
    List<MemberDto> fetch = queryFactory.select(
            Projections.constructor(MemberDto.class, member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : fetch) {
      System.out.println(memberDto);
    }
  }

  @Test
  void findUserDtoByField() {
    List<UserDto> fetch = queryFactory.select(
            Projections.fields(UserDto.class, member.username.as("name"), member.age))
        .from(member)
        .fetch();

    for (UserDto userDto : fetch) {
      System.out.println(userDto);
    }

  }

  @Test
  void findDtoByQueryProjection() {
    List<MemberDto> fetch = queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();

    for (MemberDto memberDto : fetch) {
      System.out.println(memberDto);
    }
  }

  @Test
  void dynamicQuery_booleanBuilder() {
    String usernameParam = "member1";
    int ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameCondition, Integer ageCondition) {

    BooleanBuilder builder = new BooleanBuilder();
    if(usernameCondition != null) {
      builder.and(member.username.eq(usernameCondition));
    }
    if(ageCondition != null) {
      builder.and(member.age.eq(ageCondition));
    }

    return queryFactory
        .selectFrom(member)
        .where(builder)
        .fetch();
  }

  @Test
  void dynamicQuery_whereParam() {
    String usernameParam = "member1";
    int ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember2(String usernameCondition, Integer ageCondition) {


    return queryFactory
        .selectFrom(member)
        .where(usernameEq(usernameCondition), ageEq(ageCondition))
        .fetch();
  }

  private Predicate ageEq(Integer ageCondition) {
    if(ageCondition == null) {
      return null;
    }
    return member.age.eq(ageCondition);
  }

  private Predicate usernameEq(String usernameCondition) {
    if(usernameCondition != null) {
      return member.username.eq(usernameCondition);
    }
    return null;
  }

  @Test
  void bulkUpdate() {

    long count = queryFactory.update(member)
        .set(member.username, "비회원")
        .where(member.age.lt(28))
        .execute();

    em.flush();
    em.clear();

    assertThat(count).isEqualTo(2L);
  }

  @Test
  void bulkAdd() {
    long count = queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .execute();

  }

  @Test
  void bulkDelete() {

    long execute = queryFactory
        .delete(member)
        .where(member.age.gt(18))
        .execute();
  }

  @Test
  void sqlFunction() {
    List<String> m = queryFactory.select(
            Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username,"member", "M"))
        .from(member)
        .fetch();

    for (String s : m) {
      System.out.println(s);
    }

  }

}
