package study.querydsl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> ,MemberRepositoryCustom,
    QuerydslPredicateExecutor<Member> {

  List<Member> findByUsername(String username);


}
