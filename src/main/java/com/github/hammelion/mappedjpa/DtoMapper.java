package com.github.hammelion.mappedjpa;

public interface DtoMapper<D, E> {
    E fromDto(D dto);

    D toDto(E e);
}
