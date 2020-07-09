package org.company.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.company.example.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
