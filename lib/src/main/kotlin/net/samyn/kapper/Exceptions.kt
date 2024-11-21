package net.samyn.kapper

class KapperMappingException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class KapperUnsupportedOperationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class KapperParseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class KapperResultException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
