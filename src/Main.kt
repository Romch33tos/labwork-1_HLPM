fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
        kotlin.system.exitProcess(EXIT_HELP)
    }
    if (args.contains("-h") || args.contains("--help")) {
        printHelp()
        kotlin.system.exitProcess(EXIT_HELP)
    }

    val map = parseKeyValues(args)
    if (map == null) {
        printHelp()
        kotlin.system.exitProcess(EXIT_BAD_FORMAT)
    }

    for (key in REQUIRED_KEYS) {
        if (map[key].isNullOrBlank()) {
            printHelp()
            kotlin.system.exitProcess(EXIT_BAD_FORMAT)
        }
    }

    val volume = map["--volume"]!!.toIntOrNull()
    if (volume == null) {
        printHelp()
        kotlin.system.exitProcess(EXIT_BAD_FORMAT)
    }

    val service = AccessService(
        users = StorageUserRepository(),
        resources = StorageResourceRepository(),
        rules = StorageRuleRepository()
    )

    val code = service.check(
        AccessRequest(
            login = map["--login"]!!,
            password = map["--password"]!!,
            action = map["--action"]!!,
            resourcePath = map["--resource"]!!,
            volume = volume
        )
    )
    kotlin.system.exitProcess(code)
}
