package com.voyagego.traingo.train.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.train.model.Coach;

public interface CoachRepository extends JpaRepository<Coach, Long> {

}
