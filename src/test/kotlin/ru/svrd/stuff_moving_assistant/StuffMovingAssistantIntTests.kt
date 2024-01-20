package ru.svrd.stuff_moving_assistant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod.GET
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.svrd.stuff_moving_assistant.domain.moving_box.CreateMovingBoxDto
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBox
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBoxExtras
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionDto
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionResponse
import java.net.URI.create
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [StuffMovingAssistantApplication::class, TestConfiguration::class]
)
class StuffMovingAssistantIntTests {

    @LocalServerPort
    var port: Int? = null

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    companion object {

        @Container
        @JvmStatic
        private val postgresDb = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("stuff-moving-assistant")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.master.hikari.jdbc-url", postgresDb::getJdbcUrl)
            registry.add("spring.datasource.master.hikari.username", postgresDb::getUsername)
            registry.add("spring.datasource.master.hikari.password", postgresDb::getPassword)
        }
    }

    @Test
    fun createNewBox() {
        val timeForCheck = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val sessionInfo = CreateMovingSessionDto("FirstSession")
        val apiSession = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session",
            sessionInfo,
            CreateMovingSessionResponse::class.java
        )

        val boxInfo = CreateMovingBoxDto("BoxName")
        val apiBoxes = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/newBox",
            boxInfo,
            MovingBox::class.java
        )

        assertAll(
            { assertEquals(200, apiBoxes.statusCode.value()) },
            { assertEquals("BoxName", apiBoxes.body!!.title) },
            { assertEquals(apiSession.body!!.id, apiBoxes.body!!.sessionId) },
            { assertEquals(timeForCheck, apiBoxes.body!!.createdAt.toString()) },
            { assertNotNull(apiBoxes.body!!.extras) }
        )
    }

    @Test
    fun editItemsBox() {
        val timeForCheck = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val sessionTitle = CreateMovingSessionDto("FirstSession")
        val apiSession = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session",
            sessionTitle,
            CreateMovingSessionResponse::class.java
        )

        val boxTitle = CreateMovingBoxDto("BoxName")
        val newBox = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/newBox",
            boxTitle,
            MovingBox::class.java
        )

        val items = MovingBoxExtras(items = listOf("firstItem", "secondItem"))

        val editedItemBox = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/box/${newBox.body!!.id}/editItems",
            items,
            MovingBox::class.java
        )
        assertAll(
            { assertEquals(200, editedItemBox.statusCode.value()) },
            { assertEquals("BoxName", editedItemBox.body!!.title) },
            { assertEquals(apiSession.body!!.id, editedItemBox.body!!.sessionId) },
            { assertEquals(timeForCheck, editedItemBox.body!!.createdAt.toString()) },
            { assertEquals(2, editedItemBox.body!!.extras.items!!.size) }
        )
    }

    @Test
    fun getBoxes() {
        val timeForCheck = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val sessionTitle = CreateMovingSessionDto("FirstSession")
        val apiSession = testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session",
            sessionTitle,
            CreateMovingSessionResponse::class.java
        )

        val boxOne = CreateMovingBoxDto("oneBoxName")
        testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/newBox",
            boxOne,
            MovingBox::class.java
        )

        val boxTwo = CreateMovingBoxDto("twoBoxName")
        testRestTemplate.postForEntity(
            "http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/newBox",
            boxTwo,
            MovingBox::class.java
        )

        val requestEntity = RequestEntity<Any>(
            GET,
            create("http://localhost:$port/api/v1/moving/session/${apiSession.body!!.id}/boxes")
        )

        val responseEntity: ResponseEntity<List<MovingBox>> =
            testRestTemplate.exchange(requestEntity,
                object : ParameterizedTypeReference<List<MovingBox>>() {})

        assertAll(
            { assertEquals(200, responseEntity.statusCode.value()) },
            { assertEquals(2, responseEntity.body!!.size) },
            { assertEquals("oneBoxName", responseEntity.body!!.first().title) },
            { assertEquals("twoBoxName", responseEntity.body!!.last().title) },
            { assertEquals(apiSession.body!!.id, responseEntity.body!!.first().sessionId) },
            { assertEquals(apiSession.body!!.id, responseEntity.body!!.last().sessionId) },
            { assertEquals(timeForCheck, responseEntity.body!!.first().createdAt.toString()) },
            { assertNotNull(responseEntity.body!!.first().extras) }

        )
    }
}
