package net.pbforge.crypto

private val nameAndValueRegex = Regex("[a-zA-Z0-9._-]+")

// TODO Allow for spaces and commas. In case of the later values must be quoted

interface NameBuilder : Map<String, String> {
    companion object {
        fun make(): NameBuilder = NameBuilderImpl()

        fun parse(encoded: String): NameBuilder {
            val instance = NameBuilderImpl()
            encoded
                .split(",")
                .map { it.split('=') }
                .forEach {
                    check(it.size == 2) { "Malformed input-pair in '$encoded'" }
                    instance[it[0].trim()] = it[1].trim()
                }
            return instance
        }

        fun build(builder: NameBuilder.() -> Unit): String {
            val b = make()
            builder.invoke(b)
            return b.toString()
        }
    }

    operator fun String.timesAssign(value: String) {
        this@NameBuilder[this] = value
    }

    var DN: String
    var O: String
    var L: String
    var ST: String
    var C: String
    operator fun set(key: String, value: String)
}

private class NameBuilderImpl(
    private val map: LinkedHashMap<String, String> = LinkedHashMap()
) : Map<String, String> by map, NameBuilder {


    override var DN: String
        get() = this["DN"]
        set(value) = this.set("DN", value)

    override var O: String
        get() = this["O"]
        set(value) = this.set("O", value)

    override var L: String
        get() = this["L"]
        set(value) = this.set("L", value)

    override var ST: String
        get() = this["ST"]
        set(value) = this.set("ST", value)

    override var C: String
        get() = this["C"]
        set(value) = this.set("C", value)

    override fun get(key: String): String = map[key] ?: throw NoSuchElementException()

    override fun set(key: String, value: String) {
        check(key.matches(nameAndValueRegex)) { "Name $key contains illegal characters" }
        check(value.matches(nameAndValueRegex)) { "Value $value contains illegal characters" }
        map[key] = value
    }

    override fun toString() = map.entries.joinToString(
        transform = { "${it.key}=${it.value}" },
        separator = ", "
    )
}


