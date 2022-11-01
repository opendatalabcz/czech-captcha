package cz.opendatalab.captcha.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity

import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .antMatchers("/api/verification/**", "/swagger-ui/*", "/v3/api-docs", "/v3/api-docs/*", "/index.html", "/", "/css/*").permitAll()
            .antMatchers("/api/admin/**/*").hasAuthority("ROLE_ADMIN")
            .anyRequest().authenticated()
            .and()
                .formLogin()
                .permitAll()
            .and()
                .logout()
                .permitAll()
            .and()
                .httpBasic()
        http
            .csrf().ignoringAntMatchers("/**")
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

