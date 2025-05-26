package net.samyn.kapper.internal.automapper

import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.Mapper

fun <T : Any> createAutoMapper(clazz: Class<T>): Mapper<T> {
    if (clazz.kotlin.isData) return KotlinDataClassMapper(clazz)
    if (clazz.isRecord) return RecordMapper(clazz)
    throw KapperMappingException(
        "Cannot create auto mapper for class ${clazz.name}. " +
            "Only data classes and records are supported." +
            "Use a custom mapper instead.",
    )
}
