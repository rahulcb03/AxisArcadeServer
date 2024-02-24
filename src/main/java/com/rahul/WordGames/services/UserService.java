package com.rahul.wordgames.services;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.rahul.wordgames.repos.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService{


    private final UserRepository userRepository;

    public UserDetailsService userDetailsService(){
        return new UserDetailsService() {

            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                
                return userRepository.findUserByUsername(username).orElseThrow();
            }
            
        };
    }



}
