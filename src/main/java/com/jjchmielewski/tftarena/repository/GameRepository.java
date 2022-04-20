package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.riotapi.entities.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameRepository extends MongoRepository<Game, String> {
}
