import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.required
import java.security.MessageDigest
import java.security.SecureRandom

const val EXIT_SUCCESS = 0
const val EXIT_HELP = 1
const val EXIT_BAD_PASSWORD = 2
const val EXIT_BAD_LOGIN = 3
const val EXIT_UNKNOWN_ACTION = 4
const val EXIT_NO_ACCESS = 5
const val EXIT_NO_RESOURCE = 6
const val EXIT_BAD_FORMAT = 7
const val EXIT_VOLUME_EXCEEDED = 8

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

data class User(
  val login: String,
  val salt: ByteArray,
  val passwordHash: ByteArray
)

object Storage {
  val users: List<User> = listOf(
    createUser("alice", "qwerty"),
    createUser("bob", "hunter2"),
    createUser("carol", "s3cret")
  )

  val resources: List<Resource> = listOf(
    Resource("A", "", 100),
    Resource("B", "A", 50),
    Resource("C", "A.B", 25),
    Resource("D", "A", 10)
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
      val rule = accessRules.firstOrNull { it.login == login && it.resourcePath == candidate }
      if (rule != null) return rule
    }
    return null
  }

  private fun createUser(login: String, password: String): User {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val hash = hashPassword(password, salt)
    return User(login, salt, hash)
  }

  fun hashPassword(password: String, salt: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(salt)
    return digest.digest(password.toByteArray(Charsets.UTF_8))
  }
}

object ResourceValidator {
  private val namePattern = Regex("^[A-Za-z0-9_]{1,20}$")

  fun isValidName(name: String): Boolean = namePattern.matches(name)

  fun isValidPath(path: String): Boolean {
    if (path.isEmpty()) return false
    val segments = path.split(".")
    if (segments.isEmpty()) return false
    return segments.all { isValidName(it) }
  }

  fun isValidVolume(volume: Int): Boolean = volume > 0
}
