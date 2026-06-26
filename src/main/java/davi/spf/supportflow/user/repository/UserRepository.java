package davi.spf.supportflow.user.repository;

import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByStatusNot(UserStatus status, Pageable pageable);
}
