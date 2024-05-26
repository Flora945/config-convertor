package dolores.flora.configconvertor

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConfigConvertorApplication

fun main(args: Array<String>) {
    runApplication<ConfigConvertorApplication>(*args) {
        setBannerMode(Banner.Mode.LOG)
    }
}
