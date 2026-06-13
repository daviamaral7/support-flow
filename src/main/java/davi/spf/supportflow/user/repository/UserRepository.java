package davi.spf.supportflow.user.repository;

import davi.spf.supportflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
