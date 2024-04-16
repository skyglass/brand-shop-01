package com.cakefactory.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SignupControllerTest {

    private SignupService signupService;
    private SecurityContextRepository securityContextRepository;
    private SignupController signupController;

    @BeforeEach
    void setUp() {
        signupService = mock(SignupService.class);
        securityContextRepository = mock(SecurityContextRepository.class);
        signupController = new SignupController(signupService, securityContextRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void registersUser() {
        signupController.signup("user", "password", "line1", "line2", "P1 CD");
        verify(signupService).register("user", "password", "line1", "line2", "P1 CD");
    }

    @Test
    void redirectsToHomepage() {
        String signupResponse = signupController.signup("user", "password", "line1", "line2", "P1 CD");
        assertThat(signupResponse).isEqualTo("redirect:/");
    }

    @Test
    void redirectsToLoginIfEmailIsTaken() {
        String email = "user@example.com";
        when(signupService.accountExists(email)).thenReturn(true);

        String signupResponse = signupController.signup(email, "password", "line1", "line2", "P1 CD");
        assertThat(signupResponse).isEqualTo("redirect:/login");
    }

    @Test
    void redirectsToHomepageIfAlreadySignedUp() {
        String email = "test@example.com";
        when(signupService.accountExists(email)).thenReturn(true);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(email, "", Collections.emptyList());
        ModelAndView signupResponse = signupController.signup(principal);
        assertThat(signupResponse.getViewName()).isEqualTo("redirect:/");
    }

    @Test
    void displaySignupPageIfNotYetSignedUp() {
        String email = "test@example.com";
        when(signupService.accountExists(email)).thenReturn(false);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(email, "", Collections.emptyList());
        ModelAndView signupResponse = signupController.signup(principal);
        assertThat(signupResponse.getViewName()).isEqualTo("signup");
    }

    @Test
    void setsDefaultEmailForAuthenticatedUser() {
        String email = "test@example.com";
        when(signupService.accountExists(email)).thenReturn(false);

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(email, "", Collections.emptyList());
        ModelAndView signupResponse = signupController.signup(principal);
        assertThat(signupResponse.getModel().get("email")).isEqualTo(email);
    }

    @Test
    void displaysSignupPageForNonAuthenticatedUser() {
        ModelAndView signupResponse = signupController.signup(null);
        assertThat(signupResponse.getViewName()).isEqualTo("signup");
    }
}