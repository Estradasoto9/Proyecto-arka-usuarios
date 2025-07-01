package com.projectArka.user_service.domain.port.in;

import com.projectArka.user_service.domain.model.User;
import reactor.core.publisher.Mono;

public interface ICreateUserPort {
    Mono<User> createUser(User user);
}