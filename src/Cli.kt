const val EXIT_SUCCESS = 0
const val EXIT_HELP = 1
const val EXIT_BAD_PASSWORD = 2
const val EXIT_BAD_LOGIN = 3
const val EXIT_UNKNOWN_ACTION = 4
const val EXIT_NO_ACCESS = 5
const val EXIT_NO_RESOURCE = 6
const val EXIT_BAD_FORMAT = 7
const val EXIT_VOLUME_EXCEEDED = 8

val REQUIRED_KEYS = listOf("--login", "--password", "--action", "--resource", "--volume")

fun parseKeyValues(args: Array<String>): Map<String, String>? {
    val result = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val key = args[i]
        if (!key.startsWith("--")) return null
        if (i + 1 >= args.size) return null
        val value = args[i + 1]
        if (value.startsWith("--")) return null
        result[key] = value
        i += 2
    }
    return result
}

fun printHelp() {
    println(
        """
        |Использование:
        |  java -jar app.jar --login <логин> --password <пароль> --action <действие> \
        |--resource <путь> --volume <объём>
        |
        |Поддерживаемые аргументы:
        |  -l, --login      Логин пользователя
        |  -p, --password   Пароль пользователя
        |  -a, --action     Действие над ресурсом: read, write, execute
        |  -r, --resource   Путь до ресурса, сегменты разделены точками (например A.B.C)
        |  -v, --volume     Запрашиваемый объём ресурса (целое положительное число)
        |  -h, --help       Показать эту справку
        |
        |Коды ответа:
        |  0 — успешное выполнение
        |  1 — запрошена справка
        |  2 — неверный пароль
        |  3 — неверный логин
        |  4 — неизвестное действие
        |  5 — нет доступа
        |  6 — несуществующий ресурс
        |  7 — некорректный формат ресурса или объёма
        |  8 — превышение максимального объёма
        """.trimMargin()
    )
}