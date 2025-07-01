package com.projectArka.user_service.domain.port.in;

import reactor.core.publisher.Mono;

public interface IDeleteUserPort {
    Mono<Void> deleteUserById(String id);
}