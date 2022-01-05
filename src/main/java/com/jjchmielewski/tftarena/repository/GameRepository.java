package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface GameRepository extends MongoRepository<Game, String> {
}
