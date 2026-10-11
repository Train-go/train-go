package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
