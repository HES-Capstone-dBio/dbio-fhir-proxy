package com.dbio.fhirproxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@PropertySource("classpath:security/security.properties")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final LogoutHandler logoutHandler;

    private static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    public WebSecurityConfig(LogoutHandler logoutHandler) {
        this.logoutHandler = logoutHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws ConfigurationException {
        try {
            http
                .authorizeRequests()
                .antMatchers("/").authenticated()
                .and()
                .oauth2Login()
                .and()
                .logout()
                // handle logout requests at /logout path
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                // customize logout handler to log out of Auth0
                .addLogoutHandler(logoutHandler);
        } catch (Exception e) {
            logger.error("Error configuring WebSecurityConfig");
            throw new ConfigurationException("Configuring HttpSecurity", "Issue in WebSecurityConfig configuring HttpSecurity with csrf");
        }
    }
}

