package com.redhat.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.redhat.example.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
