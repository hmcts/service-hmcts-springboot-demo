package uk.gov.hmcts.amp.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Two ways in, both from the same Entra issuer:
 * <ul>
 *   <li>a browser signs in with Entra (an authorization-code login) and keeps a session;</li>
 *   <li>a script sends a bearer token, with no session.</li>
 * </ul>
 * Everything under /api needs one of them. The page itself is public, so it can show a "Sign in" button.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/error", "/actuator/health")
                .permitAll()
                .anyRequest().authenticated())
            // The callback is /auth/callback, not Spring's default /login/oauth2/code/entra, because that is the
            // address the marketplace's Entra sign-in app is already registered for (on port 3100), so the
            // demo can sign in against the real tenant without anyone editing the app registration.
            .oauth2Login(login -> login
                .defaultSuccessUrl("/", true)
                .redirectionEndpoint(redirect -> redirect.baseUri("/auth/callback")))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // The page reads this cookie and sends it back as a header on each change. A bearer token
            // needs none of this: Spring does not ask for it on a request that carries one.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // The plain handler, not the default one that masks the token: the page sends the cookie's
                // value back as it is.
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .logout(logout -> logout.logoutSuccessUrl("/"))
            // An API call that is not signed in gets a plain 401, not a redirect to a login page it
            // cannot follow.
            .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                request -> request.getRequestURI().startsWith("/api/")));
        return http.build();
    }
}
