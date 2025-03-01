package net.samyn.kapper

import net.samyn.kapper.internal.KotlinDataClassMapper

fun <T : Any> createMapper(clazz: Class<T>): Mapper<T> {
    return KotlinDataClassMapper(clazz)
}
