package dolores.flora.configconvertor

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * @author qihuaiyuan
 * @since 2024/5/16
 */
private val LOGGER = LoggerFactory.getLogger(IndexController::class.java)

private val YAML_MAPPER = createYamlMapper()

fun createYamlMapper(): YAMLMapper {
    val mapper = YAMLMapper()
    mapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    return mapper
}


@Controller
class IndexController {

    @EventListener(ApplicationReadyEvent::class)
    fun openBrowser(event: ApplicationReadyEvent) {
        event.applicationContext.environment.also { env ->
            val port = env.getProperty("server.port", "8080")
            val url = "http://localhost:${port}/"
            LOGGER.info("Please open your browser and visit: $url")
        }
    }

    @GetMapping("/")
    fun index(model: Model): String {
        model["sourceConfig"] = ""
        model["convertedConfig"] = ""
        return "index"
    }

    @PostMapping("/convert")
    fun convert(
        @RequestParam sourceConfig: String,
        @RequestParam from: String,
        @RequestParam to: String,
        model: Model
    ): String {
        model["sourceConfig"] = sourceConfig
        if (from == to) {
            model["convertedConfig"] = sourceConfig
            return "index"
        }
        val parsedSource = parseSource(sourceConfig, from)
        model["convertedConfig"] = convert(parsedSource, to)
        model["from"] = from
        model["to"] = to
        return "index"
    }


    private fun convert(sourceConfig: Map<String, *>, type: String): String {
        return when (type) {
            "properties" -> {
                convertToPropertiesStr(sourceConfig)
            }

            "yaml" -> {
                YAML_MAPPER.writeValueAsString(sourceConfig)
            }

            else -> {
                "**Unsupported target format: ${type}**"
            }
        }
    }

}

fun parseSource(sourceConfig: String, type: String): Map<String, *> {
    return when (type) {
        "yaml" -> {
            YAML_MAPPER.readValue(sourceConfig, Map::class.java) as Map<String, *>
        }

        "properties" -> {
            parseProperties(sourceConfig)
        }

        else -> {
            throw IllegalArgumentException("Unsupported source format")
        }
    }
}
