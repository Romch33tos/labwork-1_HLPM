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
