# Анализ нарушений принципов SOLID

## S — Single Responsibility Principle

> У класса должна быть только одна причина для изменения.

### S1. `Storage` совмещает хранение, поиск и криптографию

**Место:** `Storage.kt` → `object Storage`

`Storage` выполняет три независимые роли:

1. Хранение данных в памяти (`users`, `resources`, `accessRules`).
2. Поиск (`findUser`, `findResource`, `findEffectiveRule`).
3. Криптография (`hashPassword`).

```kotlin
object Storage {
    val users: List<User> = listOf(/* ... */)
    val resources: List<Resource> = listOf(/* ... */)
    val accessRules: List<AccessRule> = listOf(/* ... */)

    fun findUser(login: String): User? = users.firstOrNull { it.login == login }
    fun findResource(path: String): Resource? = /* ... */
    fun findEffectiveRule(login: String, resourcePath: String): AccessRule? = /* ... */

    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
```

Любое изменение одной из ролей (переход на БД, смена алгоритма хеширования, изменение правил поиска) потребует правки одного и того же класса.

**Решение.** Разделить на отдельные компоненты:

- `InMemoryUserStorage`, `InMemoryResourceStorage`, `InMemoryRuleStorage` — только хранение.
- `StorageXxxRepository` (уже существуют) — поиск поверх хранилища.
- `Sha256PasswordHasher` — реализация интерфейса `PasswordHasher`.

```kotlin
// domain/repository/PasswordHasher.kt
interface PasswordHasher {
    fun hash(password: String, salt: ByteArray): ByteArray
}

// infrastructure/security/Sha256PasswordHasher.kt
class Sha256PasswordHasher : PasswordHasher {
    override fun hash(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
```
### S2. `AccessService.check` выполняет слишком много обязанностей

**Место:** `AccessService.kt` → `AccessService.check()`

Метод `check()` одновременно:

1. Валидирует формат запроса.
2. Аутентифицирует пользователя.
3. Парсит действие.
4. Ищет ресурс.
5. Ищет эффективное правило.
6. Проверяет права по действию.
7. Проверяет объём.

Семь причин для изменения — явное нарушение SRP.

```kotlin
fun check(request: AccessRequest): Int {
    if (!ResourceValidator.isValidPath(request.resourcePath) ||
        !ResourceValidator.isValidVolume(request.volume)
    ) return EXIT_BAD_FORMAT                                    // 1. валидация

    val user = users.findUser(request.login) ?: return EXIT_BAD_LOGIN
    if (!users.verifyPassword(user, request.password)) return EXIT_BAD_PASSWORD  // 2. аутентификация

    val action = Action.from(request.action) ?: return EXIT_UNKNOWN_ACTION       // 3. парсинг

    val resource = resources.findResource(request.resourcePath) ?: return EXIT_NO_RESOURCE  // 4. поиск
    val rule = rules.findEffectiveRule(request.login, request.resourcePath)
        ?: return EXIT_NO_ACCESS                                // 5. поиск правила

    val permitted = when (action) {                             // 6. проверка прав
        Action.READ -> rule.canRead
        Action.WRITE -> rule.canWrite
        Action.EXECUTE -> rule.canExecute
    }
    if (!permitted) return EXIT_NO_ACCESS

    if (request.volume > resource.maxVolume) return EXIT_VOLUME_EXCEEDED  // 7. объём

    return EXIT_SUCCESS
}
```

**Решение.** Разбить на компоненты с единственной ответственностью:

- `RequestValidator` — валидация запроса.
- `Authenticator` — аутентификация.
- `ResourceFinder` — поиск ресурса.
- `AccessResolver` — определение правила и проверка прав.
- `AccessService` — тонкий оркестратор, вызывающий их по цепочке.

```kotlin
class AccessService(
    private val validator: RequestValidator,
    private val authenticator: Authenticator,
    private val resourceFinder: ResourceFinder,
    private val accessResolver: AccessResolver
) {
    fun check(request: AccessRequest): Int {
        validator.validate(request)?.let { return it }
        val user = authenticator.authenticate(request) ?: return EXIT_BAD_LOGIN
        val resource = resourceFinder.find(request.resourcePath) ?: return EXIT_NO_RESOURCE
        return accessResolver.resolve(user, resource, request)
    }
}
```
### S3. `Main.main` совмещает парсинг, валидацию и сборку зависимостей

**Место:** `Main.kt` → `fun main()`

Функция `main()` выполняет пять разных задач:

```kotlin
fun main(args: Array<String>) {
    if (args.isEmpty()) { printHelp(); exitProcess(EXIT_HELP) }         // 1. help
    if (args.contains("-h") || args.contains("--help")) { /* ... */ }    // 1. help

    val map = parseKeyValues(args)                                       // 2. парсинг
    if (map == null) { printHelp(); exitProcess(EXIT_BAD_FORMAT) }

    for (key in REQUIRED_KEYS) {                                         // 3. валидация
        if (map[key].isNullOrBlank()) { printHelp(); exitProcess(EXIT_BAD_FORMAT) }
    }
    val volume = map["--volume"]!!.toIntOrNull() ?: run { /* ... */ }

    val service = AccessService(                                         // 4. сборка графа
        users = StorageUserRepository(),
        resources = StorageResourceRepository(),
        rules = StorageRuleRepository()
    )

    val code = service.check(AccessRequest(/* ... */))
    kotlin.system.exitProcess(code)                                      // 5. exit
}
```

**Решение.** Разделить на четыре компонента, оставив `main()` тонким:

```kotlin
fun main(args: Array<String>) {
    val parser = CliArgumentParser()
    val validator = CliArgumentValidator()

    val parsed = parser.parse(args) ?: run { HelpPrinter.print(); exitProcess(EXIT_BAD_FORMAT) }
    val request = validator.validate(parsed) ?: run { HelpPrinter.print(); exitProcess(EXIT_BAD_FORMAT) }

    val service = ApplicationFactory.create()
    exitProcess(service.check(request))
}
```
### S4. `Cli.kt` совмещает константы, парсер и справку

**Место:** `Cli.kt`

Файл содержит четыре категории кода:

```kotlin
// 1. Константы кодов возврата
const val EXIT_SUCCESS = 0
const val EXIT_HELP = 1
// ... и ещё 7 констант

// 2. Список обязательных ключей
val REQUIRED_KEYS = listOf("--login", "--password", "--action", "--resource", "--volume")

// 3. Парсер
fun parseKeyValues(args: Array<String>): Map<String, String>? { /* ... */ }

// 4. Вывод справки
fun printHelp() { /* ... */ }
```

Изменение формата справки, парсера или кодов возврата — три независимые причины для правки одного файла.

**Решение.** Разделить на три файла:

```kotlin
// presentation/cli/ExitCode.kt
enum class ExitCode(val code: Int) {
    SUCCESS(0), HELP(1), BAD_PASSWORD(2), BAD_LOGIN(3),
    UNKNOWN_ACTION(4), NO_ACCESS(5), NO_RESOURCE(6),
    BAD_FORMAT(7), VOLUME_EXCEEDED(8)
}

// presentation/cli/CliParser.kt
class CliParser {
    fun parse(args: Array<String>): Map<String, String>? { /* ... */ }
}

// presentation/cli/HelpPrinter.kt
object HelpPrinter { fun print() { /* ... */ } }
```
### S5. `User` совмещает данные и логику сравнения `ByteArray`

**Место:** `Models.kt` → `class User`

Класс хранит доменные данные (`login`, `salt`, `passwordHash`) и одновременно содержит кастомную реализацию `equals`/`hashCode` исключительно ради корректного сравнения `ByteArray`:

```kotlin
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

    override fun hashCode(): Int { /* ... */ }
}
```

Это смешение двух ролей: «модель пользователя» и «сравнение бинарных данных».

**Решение.** Ввести value-класс `PasswordHash`, который сам умеет сравниваться:

```kotlin
// domain/model/PasswordHash.kt
@JvmInline
value class PasswordHash(private val bytes: ByteArray) {
    override fun equals(other: Any?) =
        other is PasswordHash && bytes.contentEquals(other.bytes)
    override fun hashCode() = bytes.contentHashCode()
}

// domain/model/User.kt
data class User(
    val login: String,
    val salt: ByteArray,
    val passwordHash: PasswordHash
)
```

Теперь `User` можно сделать `data class` без кастомных `equals`/`hashCode`.

## O — Open/Closed Principle

> Классы должны быть открыты для расширения, но закрыты для изменения.

### O1. `when (action)` в `AccessService.check`

**Место:** `AccessService.kt` → `AccessService.check()`

```kotlin
val permitted = when (action) {
    Action.READ -> rule.canRead
    Action.WRITE -> rule.canWrite
    Action.EXECUTE -> rule.canExecute
}
```

При добавлении нового действия (например, `DELETE`) придётся:

1. Добавить `DELETE` в `enum Action`.
2. Дописать ветку `when` в `AccessService`.
3. Добавить поле `canDelete` в `AccessRule`.

Три изменения в трёх классах ради одного нового действия — класс не закрыт для модификации.

**Решение.** Вынести проверку в полиморфизм — каждая `Action` знает, как проверить себя:

```kotlin
// domain/model/Action.kt
enum class Action(private val check: (AccessRule) -> Boolean) {
    READ({ it.canRead }),
    WRITE({ it.canWrite }),
    EXECUTE({ it.canExecute });

    fun isPermittedBy(rule: AccessRule): Boolean = check(rule)

    companion object {
        fun from(value: String): Action? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

// AccessService
if (!action.isPermittedBy(rule)) return EXIT_NO_ACCESS
```

Теперь новое действие = одна строка в enum. `AccessService` не меняется.

### O2. `REQUIRED_KEYS` и строковые ключи CLI продублированы

**Место:** `Cli.kt` → `REQUIRED_KEYS`; `Main.kt` → `map["--login"]` и т.д.

Список обязательных ключей живёт в `Cli.kt`:

```kotlin
val REQUIRED_KEYS = listOf("--login", "--password", "--action", "--resource", "--volume")
```

А сами строковые литералы используются в `Main.kt`:

```kotlin
val code = service.check(
    AccessRequest(
        login = map["--login"]!!,
        password = map["--password"]!!,
        action = map["--action"]!!,
        resourcePath = map["--resource"]!!,
        volume = volume
    )
)
```

Добавление нового обязательного аргумента требует правок в двух местах.

**Решение.** Единый источник правды — `enum class`:

```kotlin
// presentation/cli/CliArgument.kt
enum class CliArgument(val key: String, val required: Boolean) {
    LOGIN("--login", true),
    PASSWORD("--password", true),
    ACTION("--action", true),
    RESOURCE("--resource", true),
    VOLUME("--volume", true),
    HELP("--help", false);

    companion object {
        val requiredKeys = entries.filter { it.required }.map { it.key }
    }
}
```

Теперь `Main.kt` обращается к `CliArgument.LOGIN.key`, а список обязательных ключей генерируется автоматически.
