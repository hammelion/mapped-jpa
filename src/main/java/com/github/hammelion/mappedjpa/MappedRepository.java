package com.github.hammelion.mappedjpa;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.vavr.control.Try;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A repository which actually creates an abstraction layer between domain objects and persistence layer entity objects.
 *
 * @param <J>  Wrapped JpaRepository
 * @param <E>  Entity object
 * @param <D>  Domain object (DTO)
 * @param <ID> Id of an entity
 */
@SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType", "unused", "deprecation"})
public class MappedRepository<J extends JpaRepository<E, ID>, D, E, ID> implements JpaRepository<D, ID> {
    protected final J repository;
    protected final DtoMapper<D, E> mapper;

    public MappedRepository(
        J repository,
        DtoMapper<D, E> mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    private static RuntimeException runtimeException(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }

    @Override
    public Optional<D> findById(ID id) {
        return callOpt(() -> repository.findById(id));
    }

    @Override
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    @Override
    public List<D> findAll() {
        return callList(repository::findAll);
    }

    @Override
    public List<D> findAll(Sort sort) {
        return callList(() -> repository.findAll(sort));
    }

    @Override
    public Page<D> findAll(Pageable pageable) {
        return callPage(() -> repository.findAll(pageable));
    }

    @Override
    public List<D> findAllById(Iterable<ID> ids) {
        return callList(() -> repository.findAllById(ids));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(D entity) {
        repository.delete(fromDto(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        repository.deleteAllById(ids);
    }

    @Override
    public void deleteAll(Iterable<? extends D> entities) {
        repository.deleteAll(fromDtoList(entities));
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public <S extends D> S save(S entity) {
        return call(() -> repository.save(fromDto(entity)));
    }

    @Override
    public <S extends D> List<S> saveAll(Iterable<S> entities) {
        return callList(() -> repository.saveAll(fromDtoList(entities)));
    }

    @Override
    public void flush() {
        repository.flush();
    }

    @Override
    public <S extends D> S saveAndFlush(S entity) {
        return call(() -> repository.saveAndFlush(fromDto(entity)));
    }

    @Override
    public <S extends D> List<S> saveAllAndFlush(Iterable<S> entities) {
        return callList(() -> repository.saveAllAndFlush(fromDtoList(entities)));
    }

    @Override
    public void deleteInBatch(Iterable<D> entities) {
        repository.deleteInBatch(fromDtoList(entities));
    }

    @Override
    public void deleteAllInBatch(Iterable<D> entities) {
        repository.deleteAllInBatch(fromDtoList(entities));
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<ID> ids) {
        repository.deleteAllByIdInBatch(ids);
    }

    @Override
    public void deleteAllInBatch() {
        repository.deleteAllInBatch();
    }

    @Override
    public D getOne(ID id) {
        return call(() -> repository.getOne(id));
    }

    @Override
    public D getById(ID id) {
        return call(() -> repository.getById(id));
    }

    @Override
    public <S extends D> Optional<S> findOne(Example<S> example) {
        return callOpt(() -> repository.findOne(fromDto(example)));
    }

    @Override
    public <S extends D> List<S> findAll(Example<S> example) {
        return this.callList(() -> repository.findAll(fromDto(example)));
    }

    @Override
    public <S extends D> List<S> findAll(
        Example<S> example,
        Sort sort
    ) {
        return this.callList(() -> repository.findAll(fromDto(example), sort));
    }

    @Override
    public <S extends D> Page<S> findAll(
        Example<S> example,
        Pageable pageable
    ) {
        return callPage(() -> repository.findAll(fromDto(example), pageable));
    }

    @Override
    public <S extends D> long count(Example<S> example) {
        return repository.count(fromDto(example));
    }

    @Override
    public <S extends D> boolean exists(Example<S> example) {
        return repository.exists(fromDto(example));
    }

    private <S extends D> List<S> callList(
        Callable<List<E>> f
    ) {
        return Try.of(() -> f.call().stream().map(e -> (S) mapper.toDto(e)).collect(Collectors.toList()))
                  .getOrElseThrow(MappedRepository::runtimeException);
    }

    private <S extends D> Optional<S> callOpt(
        Callable<Optional<E>> f
    ) {
        return Try.of(f::call)
                  .map(o -> o.map(e -> (S) mapper.toDto(e)))
                  .getOrElseThrow(MappedRepository::runtimeException);
    }

    private <S extends D> S call(Callable<E> f) {
        return Try.of(f::call)
                  .map(e -> (S) mapper.toDto(e))
                  .getOrElseThrow(MappedRepository::runtimeException);
    }

    private <S extends D> Page<S> callPage(Callable<Page<E>> f) {
        return Try.of(f::call)
                  .map(p -> p.map(e -> (S) mapper.toDto(e)))
                  .getOrElseThrow(MappedRepository::runtimeException);
    }

    public Optional<D> toDto(Optional<E> o) {
        return o.map(mapper::toDto);
    }

    public List<D> toDto(Iterable<E> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                            .map(mapper::toDto)
                            .collect(Collectors.toList());
    }

    public List<E> fromDtoList(Iterable<? extends D> dtos) {
        return StreamSupport.stream(dtos.spliterator(), false)
                            .map(this::fromDto)
                            .collect(Collectors.toList());
    }

    public E fromDto(D dto) {
        return mapper.fromDto(dto);
    }

    public <S extends D> Example<E> fromDto(Example<S> example) {
        return Example.of(fromDto(example.getProbe()));
    }
}