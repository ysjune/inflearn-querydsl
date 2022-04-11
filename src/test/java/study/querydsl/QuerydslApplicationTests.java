package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

  @Autowired
  EntityManager em;

  @Test
  void contextLoads() {
    Hello hello = new Hello();
    em.persist(hello);

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    QHello qHello = new QHello("h");

    Hello result = jpaQueryFactory
        .selectFrom(qHello)
        .fetchOne();

    assertThat(result).isEqualTo(hello);
    assertThat(result.getId()).isEqualTo(hello.getId());
  }

}
