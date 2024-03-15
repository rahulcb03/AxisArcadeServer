package com.rahul.wordgames.repos;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.rahul.wordgames.entities.User;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId>{

    Optional<User> findUserByUsername(String username);

    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByResetToken(String resetToken);


}
