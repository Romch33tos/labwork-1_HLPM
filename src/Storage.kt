import java.security.MessageDigest

fun String.hexToBytes(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()

object Storage {
    // Пароли хранятся только в виде солёных хэшей.
    // Значения получены утилитой GenHashes.kt (SHA-256, соль 16 байт).
    val users: List<User> = listOf(
        User(
            "alice",
            "df72903c72cccf1b259fab6274ae8b5d".hexToBytes(),
            "4b6b1a66e71e4489498eef4375dd4eff1ed6e809db10da98a38629a19051d5cd".hexToBytes()
        ),
        User(
            "bob",
            "4908214be0f7448a01072a1d2aa6ed5d".hexToBytes(),
            "82d7485d1e84936b193fd9ed6e324698597acdfce7a16b6ca0a2597c286b4d64".hexToBytes()
        ),
        User(
            "carol",
            "fbe50e9e4ed358e57e73a7d64eae36ad".hexToBytes(),
            "1b9d94c334c74569fb927f5c3ee7937f302b7a1e7f03558a6757a98e98ae7751".hexToBytes()
        )
    )

    val resources: List<Resource> = listOf(
        Resource("A", "", 100),
        Resource("B", "A", 50),
        Resource("C", "A.B", 25),
        Resource("D", "A", 10),
        Resource("E", "", 30)
    )

    val accessRules: List<AccessRule> = listOf(
        AccessRule("alice", "A", canRead = true, canWrite = true, canExecute = true),
        AccessRule("alice", "A.B", canRead = true, canWrite = false, canExecute = false),
        AccessRule("alice", "A.B.C", canRead = false, canWrite = true, canExecute = false),
        AccessRule("bob", "A", canRead = true, canWrite = false, canExecute = false),
        AccessRule("bob", "A.D", canRead = true, canWrite = true, canExecute = true),
        AccessRule("carol", "A.B.C", canRead = true, canWrite = true, canExecute = true)
    )

    fun findUser(login: String): User? = users.firstOrNull { it.login == login }

    fun findResource(path: String): Resource? = resources.firstOrNull { it.fullPath == path }

    fun findEffectiveRule(login: String, resourcePath: String): AccessRule? {
        val segments = resourcePath.split(".")
        for (length in segments.size downTo 1) {
            val candidate = segments.subList(0, length).joinToString(".")
            accessRules.firstOrNull { it.login == login && it.resourcePath == candidate }?.let { return it }
        }
        return null
    }

    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}