package com.cakefactory.auth;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/signup")
class SignupController {

    private final SignupService signupService;

    private final SecurityContextRepository securityContextRepository;

    public SignupController(SignupService signupService, SecurityContextRepository securityContextRepository) {
        this.signupService = signupService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    ModelAndView signup(Principal principal) {
        if (principal != null && signupService.accountExists(principal.getName())) {
            return new ModelAndView("redirect:/");
        }

        return new ModelAndView("signup", Map.of("email", principal == null ? "" : principal.getName()));
    }

    @PostMapping
    String signup(String email, String password, String addressLine1, String addressLine2, String postcode) {
        if (this.signupService.accountExists(email)) {
            return "redirect:/login";
        }

        this.signupService.register(email, password, addressLine1, addressLine2, postcode);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(token);
        //spring security 6.x requires saving context explicitly after changes
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        securityContextRepository.saveContext(securityContext, attributes.getRequest(), attributes.getResponse());
        return "redirect:/";
    }
}
