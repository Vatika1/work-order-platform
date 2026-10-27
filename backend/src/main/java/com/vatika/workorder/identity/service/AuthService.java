package com.vatika.workorder.identity.service;

import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.identity.dto.LoginRequest;
import com.vatika.workorder.identity.dto.RegisterRequest;
import com.vatika.workorder.identity.dto.TokenResponse;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.shared.security.JwtProperties;
import com.vatika.workorder.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .filter(User::isEnabled)
                .filter(u-> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String token = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getClientId());

        return new TokenResponse(token, jwtProperties.accessTtl().toSeconds());
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (clientRepository.existsByName(request.companyName())) {
            throw new IllegalArgumentException("Company name already taken");
        }

        Client client = new Client(request.companyName());
        clientRepository.save(client);

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                Role.CLIENT_ADMIN,
                client.getId()
        );
        userRepository.save(user);

        String token = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getClientId());
        return new TokenResponse(token, jwtProperties.accessTtl().toSeconds());
    }
}
