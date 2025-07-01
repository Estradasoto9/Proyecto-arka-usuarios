package com.projectArka.user_service.domain.port.in;

import com.projectArka.user_service.domain.model.User;
import reactor.core.publisher.Mono;

public interface IUpdateUserPort {
    Mono<User> updateUser(User user);
}