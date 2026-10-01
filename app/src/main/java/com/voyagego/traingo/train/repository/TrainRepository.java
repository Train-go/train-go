package com.voyagego.traingo.train.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.train.model.Train;

public interface TrainRepository extends JpaRepository<Train, Long> {

}
