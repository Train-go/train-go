package com.voyagego.traingo.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.auth.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
