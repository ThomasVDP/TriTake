package com.github.ThomasVDP.tritake.event;

import discord4j.core.event.domain.Event;
import org.reactivestreams.Publisher;

public interface IEvent<T extends Event>
{
    String getName();

    Class<? extends Object> getClassType();

    boolean canExecute(T event);

    Publisher<?> execute(T event);
}
