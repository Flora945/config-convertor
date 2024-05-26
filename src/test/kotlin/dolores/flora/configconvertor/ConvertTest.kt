package dolores.flora.configconvertor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author qihuaiyuan
 * @since 2024/5/26
 */
class ConvertTest {

    @Test
    fun parseProperties() {
        val src = """
            key1=value1
            key2=value2
            key3=value3
            key4[0]=value4
            key4[1]=value5
            key5.age=value7
            key5.age.name=value11
            key5.name=value6
            key5.name.age=value10
            key6[0][0].age.name=value13
            key6[0][0].name.age=value12
            key6[0][1].age=15
            key6[0][1].name=value14
        """.trimIndent()
        val parsed = parseProperties(src)

        val regenerated = StringBuilder().apply {
            for ((k, v) in convertToProperties(parsed)) {
                append("$k=$v\n")
            }
        }.also(::print)
            .toString().dropLast(1)

        assertEquals(src, regenerated)
    }

    @Test
    fun yaml2prop2yaml() {
        val src = """
root:
  level1:
    level2:
      level3: value
      array:
        - item1
        - item2
        - item3
    array:
      - name: flora
        age: 30
    array2:
      - - name: flora
          age: 30
        - name: flora
          age: 30
      - - name: flora
          age: 30
        - name: flora
          age: 30
spring.application:
  name: test
  version: 1.0.0
spring:
  yes: true
        """.trimIndent()

        val parsedFromYaml = parseSource(src, "yaml");
        val props1 = convertToProperties(parsedFromYaml)
        val serialized = StringBuilder().apply {
            for ((k, v) in props1) {
                append("$k = $v\n")
            }
        }.also {
            print(it)
        }.toString()
        val parsedProperties = parseProperties(serialized)
        assertEquals(props1, convertToProperties(parsedProperties))
    }
}
