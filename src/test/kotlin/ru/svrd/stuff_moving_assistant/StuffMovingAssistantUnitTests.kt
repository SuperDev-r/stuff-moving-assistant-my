package ru.svrd.stuff_moving_assistant

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import ru.svrd.stuff_moving_assistant.application.api.MovingControllerV1
import ru.svrd.stuff_moving_assistant.domain.moving_box.CreateMovingBoxDto
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBox
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBoxExtras
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBoxService
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionDto
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionResponse
import ru.svrd.stuff_moving_assistant.domain.moving_session.MovingSession
import ru.svrd.stuff_moving_assistant.domain.moving_session.MovingSessionService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@WebMvcTest(MovingControllerV1::class)
class StuffMovingAssistantAppTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var movingSessionService: MovingSessionService

    @MockBean
    lateinit var movingBoxService: MovingBoxService


    @Test
    fun successCreateNewSession() {
        val createSessionTime = LocalDate.now()
        val timeForCheck = createSessionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        `when`(movingSessionService.newSession("FirstSession", 1)).thenReturn(
            CreateMovingSessionResponse(
                id = 1,
                title = "FirstSession",
                createdAt = createSessionTime
            )
        )
        val body = CreateMovingSessionDto("FirstSession")
        mvc.perform(
            post("/api/v1/moving/session").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpectAll(
                status().isOk,
                jsonPath("$.id").value(1),
                jsonPath("$.title").value("FirstSession"),
                jsonPath("$.createdAt").value(timeForCheck)
            )
    }

    @Test
    fun failCreateNewSession() {
        `when`(movingSessionService.newSession("SecondSession", 2)).thenReturn(
            CreateMovingSessionResponse(
                id = 100,
                title = "SecondSession",
                createdAt = LocalDate.now()
            )
        )
        mvc.perform(
            post("/api/v1/moving/session").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString("test"))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun getBoxesInSession() {
        val createSessionTime = LocalDate.now()
        `when`(movingSessionService.getSession(1)).thenReturn(
            MovingSession(
                id = 100,
                ownerId = 2,
                title = "SecondSession",
                createdAt = LocalDate.now()
            )
        )

        `when`(movingBoxService.findBoxesInSession(1, "SecondBox")).thenReturn(
            listOf(
                MovingBox(
                    id = 2,
                    sessionId = 1,
                    title = "SecondBox",
                    imageURL = null,
                    qrURL = null,
                    archived = false,
                    updatedAt = createSessionTime,
                    createdAt = createSessionTime,
                    extras = MovingBoxExtras(listOf("value"))
                )
            )
        )

        mvc.perform(get("/api/v1/moving/session/1/boxes").param("item", "SecondBox"))
            .andExpectAll(
                status().isOk,
                jsonPath("$[0].id").value(2),
                jsonPath("$[0].sessionId").value(1),
                jsonPath("$[0].title").value("SecondBox"),
                jsonPath("$[0].archived").value(false)
            )
    }

    @Test
    fun getBoxFromList() {
        val createSessionTime = LocalDate.now()
        `when`(movingBoxService.findBoxesInSession(2, "FirstBox")).thenReturn(
            listOf(
                MovingBox(
                    id = 1,
                    sessionId = 2,
                    title = "FirstBox",
                    imageURL = null,
                    qrURL = "https://url.ru/picture_1.svg",
                    archived = true,
                    updatedAt = createSessionTime,
                    createdAt = createSessionTime,
                    extras = MovingBoxExtras(listOf("value_1"))
                ),
                MovingBox(
                    id = 2,
                    sessionId = 2,
                    title = "SecondBox",
                    imageURL = null,
                    qrURL = "https://url.ru/picture_2.svg",
                    archived = false,
                    updatedAt = createSessionTime,
                    createdAt = createSessionTime,
                    extras = MovingBoxExtras(listOf("value_2"))
                )
            )
        )

        mvc.perform(get("/api/v1/moving/session/2/boxes").param("item", "FirstBox"))
            .andExpectAll(
                status().isOk,
                jsonPath("$[0].id").value(1),
                jsonPath("$[0].sessionId").value(2),
                jsonPath("$[0].title").value("FirstBox"),
                jsonPath("$[0].archived").value(true),
                jsonPath("$[1].id").value(2),
                jsonPath("$[1].sessionId").value(2),
                jsonPath("$[1].title").value("SecondBox"),
                jsonPath("$[1].archived").value(false)
            )
    }

    @Test
    fun findNonExistentBoxes() {
        val createSessionTime = LocalDate.now()
        `when`(movingBoxService.findBoxesInSession(5, "SecondBox")).thenReturn(
            listOf(
                MovingBox(
                    id = 2,
                    sessionId = 5,
                    title = "FirstBox",
                    imageURL = null,
                    qrURL = "https://url.ru/picture_1.svg",
                    archived = false,
                    updatedAt = createSessionTime,
                    createdAt = createSessionTime,
                    extras = MovingBoxExtras(listOf("value_1"))
                ),
                MovingBox(
                    id = 2,
                    sessionId = 5,
                    title = "SecondBox",
                    imageURL = null,
                    qrURL = "https://url.ru/picture_2.svg",
                    archived = false,
                    updatedAt = createSessionTime,
                    createdAt = createSessionTime,
                    extras = MovingBoxExtras(listOf("value_2"))
                )
            )
        )

        mvc.perform(get("/api/v1/moving/session/5/boxes").param("item", "ThirdBox"))
            .andExpectAll(
                status().isOk,
                jsonPath("$").isEmpty
            )
    }

    @Test
    fun addNewBoxAndCheck() {
        val createSessionTime = LocalDate.now()
        val timeForCheck = createSessionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        `when`(movingSessionService.newSession("FirstSession")).thenReturn(
            CreateMovingSessionResponse(
                id = 5,
                title = "FirstSession",
                createdAt = createSessionTime
            )
        )

        `when`(movingBoxService.newBox(5, "NewFirstBox")).thenReturn(
            MovingBox(
                id = 2,
                sessionId = 5,
                title = "NewFirstBox",
                imageURL = null,
                qrURL = "https://url.ru/picture.svg",
                archived = false,
                updatedAt = createSessionTime,
                createdAt = createSessionTime,
                extras = MovingBoxExtras(listOf("value"))
            )
        )

        `when`(movingBoxService.newBox(1, "NewSecondBox")).thenReturn(
            MovingBox(
                id = 3,
                sessionId = 1,
                title = "NewSecondBox",
                imageURL = null,
                qrURL = "https://url.ru/picture.svg",
                archived = false,
                updatedAt = createSessionTime,
                createdAt = createSessionTime,
                extras = MovingBoxExtras(listOf("value"))
            )
        )

        val body = CreateMovingBoxDto("NewFirstBox")
        mvc.perform(
            post("/api/v1/moving/session/5/newBox").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpectAll(
                status().isOk,
                jsonPath("$.id").value(2),
                jsonPath("$.title").value("NewFirstBox"),
                jsonPath("$.createdAt").value(timeForCheck),
                jsonPath("$.sessionId").value(5),
                jsonPath("$.archived").value(false)
            )

        val bodySecond = CreateMovingBoxDto("NewSecondBox")
        mvc.perform(
            post("/api/v1/moving/session/1/newBox").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bodySecond))
        )
            .andExpectAll(
                status().isOk,
                jsonPath("$.id").value(3),
                jsonPath("$.title").value("NewSecondBox"),
                jsonPath("$.createdAt").value(timeForCheck),
                jsonPath("$.sessionId").value(1),
                jsonPath("$.archived").value(false)
            )
    }

    @Test
    fun editItemsBox() {
        val createSessionTime = LocalDate.now()
        val timeForCheck = createSessionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        `when`(movingBoxService.newBox(1, "NewFirstBox")).thenReturn(
            MovingBox(
                id = 1,
                sessionId = 1,
                title = "NewFirstBox",
                imageURL = null,
                qrURL = "https://url.ru/picture.svg",
                archived = false,
                updatedAt = createSessionTime,
                createdAt = createSessionTime,
                extras = MovingBoxExtras(listOf("value_first"))
            )
        )

        val extras = MovingBoxExtras(listOf("value_second"))

        `when`(movingBoxService.editExtras(1, 1, extras)).thenReturn(
            MovingBox(
                id = 1,
                sessionId = 1,
                title = "NewFirstBox",
                imageURL = null,
                qrURL = "https://url.ru/picture.svg",
                archived = false,
                updatedAt = createSessionTime,
                createdAt = createSessionTime,
                extras = MovingBoxExtras(listOf("value_second"))
            )
        )

        mvc.perform(
            post("/api/v1/moving/session/1/box/1/editItems").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(extras))
        )
            .andExpectAll(
                status().isOk,
                jsonPath("$.id").value(1),
                jsonPath("$.title").value("NewFirstBox"),
                jsonPath("$.createdAt").value(timeForCheck),
                jsonPath("$.sessionId").value(1),
                jsonPath("$.archived").value(false),
                jsonPath("$.extras.items[0]").value("value_second")
            )
    }
}