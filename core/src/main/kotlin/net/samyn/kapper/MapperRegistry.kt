package net.samyn.kapper

import java.util.concurrent.ConcurrentHashMap

/**
 * A registry for mappers.
 *
 * This registry allows for custom mappers to registered for specific classes.
 * When no custom mapper is found, Kapper will default to its auto-mapper.
 *
 * Custom mappers can be used when the auto-mapper is available for the target class or types,
 * or when a marginal performance improvement is sought by avoiding reflection used by the auto-mappers.
 */
class MapperRegistry {
    private val registry = ConcurrentHashMap<Class<*>, Mapper<*>>()

    /**
     * Registers a mapper for a specific class.
     *
     * @param mapper The mapper to register.
     * @throws IllegalStateException if a mapper for the class is already registered.
     */
    inline fun <reified T : Any> register(mapper: Mapper<T>) = register(T::class.java, mapper)

    /**
     * Registers a mapper for a specific class.
     *
     * @param clazz The class for which the mapper is registered.
     * @param mapper The mapper to register.
     * @throws IllegalStateException if a mapper for the class is already registered.
     */
    fun <T : Any> register(
        clazz: Class<T>,
        mapper: Mapper<T>,
    ) {
        check(registry.putIfAbsent(clazz, mapper) == null) {
            "Mapper for class $clazz is already registered."
        }
    }

    /**
     * Registers a mapper for a specific class if no mapper is already registered.
     *
     * @param mapper The mapper to register.
     * @throws IllegalStateException if a mapper for the type is already registered with a different mapper class.
     */
    inline fun <reified T : Any> registerIfAbsent(mapper: Mapper<T>) = registerIfAbsent(T::class.java, mapper)

    /**
     * Registers a mapper for a specific class if no mapper is already registered.
     *
     * @param clazz The class for which the mapper is registered.
     * @param mapper The mapper to register.
     * @throws IllegalStateException if a mapper for the type is already registered with a different mapper class.
     */
    fun <T : Any> registerIfAbsent(
        clazz: Class<T>,
        mapper: Mapper<T>,
    ) {
        val existing = registry.putIfAbsent(clazz, mapper)
        check(!(existing != null && existing.javaClass !== mapper.javaClass)) {
            "Mapper for class $clazz is already registered with a different instance."
        }
    }

    /**
     * Retrieves a mapper for a specific class.
     *
     * If no custom mapper is found, the auto-mapper is created and registered.
     */
    fun <T : Any> get(clazz: Class<T>): Mapper<T> {
        @Suppress("UNCHECKED_CAST")
        return registry.computeIfAbsent(clazz) { createAutoMapper(it) } as Mapper<T>
    }
}
