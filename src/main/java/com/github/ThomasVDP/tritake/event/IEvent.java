package com.github.ThomasVDP.tritake.event;

import discord4j.core.event.domain.Event;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface IEvent<T extends Event>
{
    Class<?> getClassType();

    Mono<Boolean> canExecute(T event);

    Publisher<Object> execute(T event);
}
