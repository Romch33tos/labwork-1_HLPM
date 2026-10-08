import java.security.MessageDigest

interface UserRepository {
    fun findUser(login: String): User?
    fun verifyPassword(user: User, password: String): Boolean
}

interface ResourceRepository {
    fun findResource(path: String): Resource?
}

interface RuleRepository {
    fun findEffectiveRule(login: String, resourcePath: String): AccessRule?
}

class AccessService(
    private val users: UserRepository,
    private val resources: ResourceRepository,
    private val rules: RuleRepository
) {
    fun check(request: AccessRequest): Int {
        if (!ResourceValidator.isValidPath(request.resourcePath) ||
            !ResourceValidator.isValidVolume(request.volume)
        ) {
            return EXIT_BAD_FORMAT
        }

        val user = users.findUser(request.login) ?: return EXIT_BAD_LOGIN
        if (!users.verifyPassword(user, request.password)) return EXIT_BAD_PASSWORD

        val action = Action.from(request.action) ?: return EXIT_UNKNOWN_ACTION

        val resource = resources.findResource(request.resourcePath) ?: return EXIT_NO_RESOURCE
        val rule = rules.findEffectiveRule(request.login, request.resourcePath)
            ?: return EXIT_NO_ACCESS

        val permitted = when (action) {
            Action.READ -> rule.canRead
            Action.WRITE -> rule.canWrite
            Action.EXECUTE -> rule.canExecute
        }
        if (!permitted) return EXIT_NO_ACCESS

        if (request.volume > resource.maxVolume) return EXIT_VOLUME_EXCEEDED

        return EXIT_SUCCESS
    }
}

class StorageUserRepository : UserRepository {
    override fun findUser(login: String): User? = Storage.findUser(login)

    override fun verifyPassword(user: User, password: String): Boolean =
        MessageDigest.isEqual(
            user.passwordHash,
            Storage.hashPassword(password, user.salt)
        )
}

class StorageResourceRepository : ResourceRepository {
    override fun findResource(path: String): Resource? = Storage.findResource(path)
}

class StorageRuleRepository : RuleRepository {
    override fun findEffectiveRule(login: String, resourcePath: String): AccessRule? =
        Storage.findEffectiveRule(login, resourcePath)
}