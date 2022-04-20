package com.example.captcha.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity

import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import java.lang.Exception
import org.springframework.security.provisioning.InMemoryUserDetailsManager

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.userdetails.User
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.ArrayList

import org.springframework.security.core.userdetails.UserDetails

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
//            .antMatchers("/**").permitAll()
            .antMatchers("/api/verification/*", "/swagger-ui/*", "/v3/api-docs", "/v3/api-docs/*").permitAll()
            .antMatchers("/api/admin/**/*").hasAuthority("ROLE_ADMIN")
            .anyRequest().authenticated()
            .and()
                .formLogin()
//            .loginPage("/login")
                .permitAll()
            .and()
                .logout()
                .permitAll()
            .and()
                .httpBasic()
        // todo is this necessary?
        http
            .csrf().ignoringAntMatchers("/**")
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(inMemoryUserDetailsManager())
    }

    @Bean
    fun inMemoryUserDetailsManager(): InMemoryUserDetailsManager {
        val userDetailsList: MutableList<UserDetails> = ArrayList()
        userDetailsList.add(
            User.withUsername("system").password(passwordEncoder().encode("pass"))
                .roles("ADMIN", "USER").build()
        )
        userDetailsList.add(
            User.withUsername("user1").password(passwordEncoder().encode("pass"))
                .roles("USER").build()
        )

        return InMemoryUserDetailsManager(userDetailsList)
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

