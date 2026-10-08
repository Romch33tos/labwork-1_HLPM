data class Resource(
    val name: String,
    val parentPath: String,
    val maxVolume: Int
) {
    val fullPath: String
        get() = if (parentPath.isEmpty()) name else "$parentPath.$name"
}

data class AccessRule(
    val login: String,
    val resourcePath: String,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean
)

class User(
    val login: String,
    val salt: ByteArray,
    val passwordHash: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return login == other.login &&
            salt.contentEquals(other.salt) &&
            passwordHash.contentEquals(other.passwordHash)
    }

    override fun hashCode(): Int {
        var result = login.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + passwordHash.contentHashCode()
        return result
    }
}

enum class Action {
    READ, WRITE, EXECUTE;

    companion object {
        fun from(value: String): Action? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class AccessRequest(
    val login: String,
    val password: String,
    val action: String,
    val resourcePath: String,
    val volume: Int
)
