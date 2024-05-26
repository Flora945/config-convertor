package dolores.flora.configconvertor

/**
 * @author qihuaiyuan
 * @since 2024/5/18
 */
fun convertToProperties(source: Map<String, *>): Map<String, String> {
    return resolveMap(source)
        .mapKeys { it.key.removePrefix(".") }
        .toSortedMap();
}

fun convertToPropertiesStr(source: Map<String, *>): String {
    return convertToProperties(source)
        .map { (key, value) -> "$key=$value" }
        .joinToString("\n")
}

fun parseProperties(source: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    source.lines().sorted().forEach { line ->
        if (line.isBlank() || line.startsWith("#")) {
            return@forEach
        }
        val (key, value) = line.split("=", limit = 2).map { it.trim() }
        val keys = key.split(".").filter { it.isNotBlank() }
        var current = result
        for ((index, k) in keys.withIndex()) {
            if (index == keys.size - 1) {
                ARRAY_KEY_REGEX.find(k)?.let {
                    // it's something like "config[0] = value"
                    val (_, actualKey) = it.groupValues
                    val list = extractList(k, actualKey, current)
                    list.add(value)
                } ?: run {
                    // it's something like "config[0].name = value"
                    when (current[k]) {
                        is List<*> -> {
                            val aggVal = AggregatedValue(value, current[k] as List<*>, null)
                            current[k] = aggVal
                        }

                        is Map<*, *> -> {
                            val aggVal = AggregatedValue(value, null, current[k] as Map<String, *>)
                            current[k] = aggVal
                        }

                        is AggregatedValue -> {
                            val aggVal = current[k] as AggregatedValue
                            aggVal.value = value
                        }

                        else -> {
                            current[k] = value
                        }
                    }
                }
            } else {
                current = parseLeadingPart(k, current)
            }
        }
    }
    return result
}

private val ARRAY_KEY_REGEX = """^(.+?)(\[(\d+?)])+$""".toRegex()
private val ARRAY_INDEX_REGEX = """\[(\d+?)]""".toRegex()

private fun parseLeadingPart(
    k: String,
    current: MutableMap<String, Any>
): MutableMap<String, Any> {
    var current1 = current
    ARRAY_KEY_REGEX.find(k)?.let {
        // k is something like "config[0]" or "config[0][1]"
        val (_, actualKey) = it.groupValues
        val idxes = extractIndexes(k)
        val list = getList(current1, actualKey, idxes)
        if (list.size == idxes.last()) {
            current1 = mutableMapOf()
            list.add(current1)
        } else {
            current1 = list[idxes.last()] as MutableMap<String, Any>
        }
    } ?: run {
        current1 = current1.getOrPut(k) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
    }
    return current1
}

private fun extractList(
    key: String,
    actualKey: String,
    current: MutableMap<String, Any>
): MutableList<Any> {
    val idxes = extractIndexes(key)
    val list = getList(current, actualKey, idxes)
    return list
}

private fun extractIndexes(key: String): List<Int> {
    return ARRAY_INDEX_REGEX.findAll(key)
        .map { it.groupValues[1].toInt() }
        .toList()
}

fun getList(root: MutableMap<String, Any>, key: String, idxes: List<Int>): MutableList<Any> {
    var current = root.getOrPut(key) { mutableListOf<Any>() } as MutableList<Any>
    if (idxes.size == 1) {
        return current;
    }
    for (idx in idxes.slice(0 until idxes.size - 1)) {
        if (current.size == idx) {
            current.add(mutableListOf<Any>())
        }
        current = when (current[idx]) {
            is List<*> -> {
                current[idx] as MutableList<Any>
            }

            is AggregatedValue -> {
                val aggVal = current[idx] as AggregatedValue
                aggVal.list as MutableList<Any>
            }

            is Map<*, *> -> {
                val list = mutableListOf<Any>()
                val aggVal = AggregatedValue(null, list, current[idx] as Map<String, *>)
                current[idx] = aggVal
                list
            }

            else -> {
                throw IllegalArgumentException("Invalid list structure" + current[idx])
            }
        }
    }
    return current
}

private fun resolveMap(source: Map<String, *>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    source.toSortedMap().forEach { (key, value) ->
        when (value) {
            is Map<*, *> -> {
                result.putAll(resolveMap(value as Map<String, *>).mapKeys { ".$key${it.key}" })
            }

            is List<*> -> {
                result.putAll(resolveList(value).mapKeys { ".$key${it.key}" })
            }

            is AggregatedValue -> {
                if (value.list != null) {
                    result.putAll(resolveList(value.list!!).mapKeys { ".$key${it.key}" })
                }
                if (value.map != null) {
                    result.putAll(resolveMap(value.map!!).mapKeys { ".$key${it.key}" })
                }
                if (value.value != null) {
                    result[".$key"] = value.value.toString()
                }
            }

            else -> {
                result[".$key"] = value.toString()
            }
        }
    }
    return result
}

private fun resolveList(source: List<*>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for ((index, item) in source.withIndex()) {
        when (item) {
            is Map<*, *> -> {
                result.putAll(resolveMap(item as Map<String, *>).mapKeys { "[$index]${it.key}" })
            }

            is List<*> -> {
                result.putAll(resolveList(item).mapKeys { "[$index]${it.key}" })
            }

            else -> {
                result["[$index]"] = item.toString()
            }
        }
    }
    return result
}
