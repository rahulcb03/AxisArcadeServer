package com.rahul.wordgames.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.rahul.wordgames.dto.UserProfile;
import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
public class UserService{

    @Autowired
    private UserRepository userRepository;

    public UserDetailsService userDetailsService(){
        return new UserDetailsService() {

            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                
                return userRepository.findUserByUsername(username).orElseThrow();
            }
            
        };
    }

    public User getUser(String username){
        return userRepository.findUserByUsername(username).orElseThrow();
    }

    public UserProfile createProfile(String username) {
        User user = userRepository.findUserByUsername(username).orElseThrow();

        return new UserProfile(user.getId(), username);

        
    }



}
