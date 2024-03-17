package com.rahul.wordgames.entities;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "User")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
    @Id
    private String id; 

    private String username; 
    private String password;
    private String email;  
    private String resetToken; 
    private Instant resetTimeStamp; 
    
    


    public User( String username, String password, String email){
        
        this.username = username; 
        this.password = password; 
        this.email = email; 
        
    }



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("USER"));
    }



    @Override
    public boolean isAccountNonExpired() {
        return true; 
    }



    @Override
    public boolean isAccountNonLocked() {
        return true; 
    }



    @Override
    public boolean isCredentialsNonExpired() {
      return true; 
    }



    @Override
    public boolean isEnabled() {
       return true; 
    }

    
}
