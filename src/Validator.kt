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