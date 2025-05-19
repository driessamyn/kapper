package net.samyn.kapper

import net.samyn.kapper.internal.automapper.KotlinDataClassMapper

fun <T : Any> createAutoMapper(clazz: Class<T>): Mapper<T> {
    return KotlinDataClassMapper(clazz)
}
