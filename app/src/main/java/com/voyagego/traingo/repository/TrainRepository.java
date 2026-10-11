package com.voyagego.traingo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Train;

public interface TrainRepository extends JpaRepository<Train, Long> {

}
