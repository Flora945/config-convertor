package dolores.flora.configconvertor

/**
 * @author qihuaiyuan
 * @since 2024/5/26
 */
data class AggregatedValue(
    var value: Any?,
    var list: List<*>?,
    var map: Map<String, *>?
)
