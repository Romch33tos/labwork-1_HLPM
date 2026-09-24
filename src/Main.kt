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
