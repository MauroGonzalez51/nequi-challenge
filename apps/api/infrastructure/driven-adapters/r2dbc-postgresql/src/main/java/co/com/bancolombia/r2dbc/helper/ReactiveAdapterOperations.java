package co.com.bancolombia.r2dbc.helper;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.Example;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.util.function.Function;

/**
 * Base of the repository adapters. Translates between domain models (E) and R2DBC entities (D) and
 * sends every database call through PersistenceResilience.
 * E: domain model. D: entity. I: id type. R: Spring Data repository.
 * The mapper fills objects through setters, so models and entities both need them.
 */
public abstract class ReactiveAdapterOperations<E, D, I, R extends ReactiveCrudRepository<D, I> & ReactiveQueryByExampleExecutor<D>> {
    protected R repository;
    protected ObjectMapper mapper;
    protected final PersistenceResilience resilience;
    private final Class<D> dataClass;
    private final Function<D, E> toEntityFn;

    @SuppressWarnings("unchecked")
    protected ReactiveAdapterOperations(R repository, ObjectMapper mapper, Function<D, E> toEntityFn,
            PersistenceResilience resilience) {
        this.repository = repository;
        this.mapper = mapper;
        this.resilience = resilience;
        // Reads D from the subclass declaration (extends ReactiveAdapterOperations<Franchise, FranchiseEntity, ...>).
        // Type erasure hides it at runtime otherwise, and toData needs the entity class.
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        this.dataClass = (Class<D>) genericSuperclass.getActualTypeArguments()[1];
        this.toEntityFn = toEntityFn;
    }

    // Domain model -> R2DBC entity, before saving.
    protected D toData(E entity) {
        return mapper.map(entity, dataClass);
    }

    // Despite the name, this goes from the R2DBC entity (D) to the domain model (E), using the function
    // each adapter passes to the constructor.
    protected E toEntity(D data) {
        return data != null ? toEntityFn.apply(data) : null;
    }

    // Saves and maps the stored row back, so generated values (the id) reach the caller.
    // Goes through write(): timeout and circuit breaker, no retry.
    public Mono<E> save(E entity) {
        return resilience.write(() -> saveData(toData(entity)))
                .map(this::toEntity);
    }

    protected Flux<E> saveAllEntities(Flux<E> entities) {
        return resilience.writeMany(() -> saveData(entities.map(this::toData)))
                .map(this::toEntity);
    }

    protected Mono<D> saveData(D data) {
        return repository.save(data);
    }

    protected Flux<D> saveData(Flux<D> data) {
        return repository.saveAll(data);
    }

    public Mono<E> findById(I id) {
        return resilience.read(() -> repository.findById(id)).map(this::toEntity);
    }

    public Flux<E> findByExample(E entity) {
        return resilience.readMany(() -> repository.findAll(Example.of(toData(entity))))
                .map(this::toEntity);
    }

    public Flux<E> findAll() {
        return resilience.readMany(() -> repository.findAll())
                .map(this::toEntity);
    }
}
