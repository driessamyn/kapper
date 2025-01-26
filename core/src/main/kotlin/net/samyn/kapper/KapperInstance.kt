package net.samyn.kapper

object KapperInstance {
    private val impl: Kapper = Kapper.getInstance()

    /**
     * Get a Kapper singleton instance.
     */
    fun get() = impl
}
