package ru.svrd.stuff_moving_assistant.application.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.svrd.stuff_moving_assistant.domain.moving_box.CreateMovingBoxDto
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBox
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBoxExtras
import ru.svrd.stuff_moving_assistant.domain.moving_box.MovingBoxService
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionDto
import ru.svrd.stuff_moving_assistant.domain.moving_session.CreateMovingSessionResponse
import ru.svrd.stuff_moving_assistant.domain.moving_session.MovingSessionService

@RestController
@RequestMapping("/api/v1/moving")
class MovingControllerV1(
    private val movingSessionService: MovingSessionService,
    private val movingBoxService: MovingBoxService,
) {

    @PostMapping("/session")
    fun createNewMovingSession(@RequestBody movingSession: CreateMovingSessionDto): CreateMovingSessionResponse {
        return movingSessionService.newSession(movingSession.title, ownerId = 1)
    }

    @PostMapping("/session/{sessionId}/newBox")
    fun createNewMovingBox(
        @PathVariable("sessionId") sessionId: Long,
        @RequestBody movingBox: CreateMovingBoxDto,
    ): MovingBox {
        return movingBoxService.newBox(sessionId, movingBox.title)
    }

    @GetMapping("/session/{sessionId}/boxes")
    fun getBoxesInSession(
        @PathVariable("sessionId") sessionId: Long,
        @RequestParam("item", required = false) item: String?
    ): List<MovingBox> {
        movingSessionService.getSession(sessionId) // проверяем юзера и т.д
        return movingBoxService.findBoxesInSession(sessionId, item)
    }

    @PostMapping("/session/{sessionId}/box/{boxId}/editItems")
    fun editBoxItems(
        @PathVariable("sessionId") sessionId: Long,
        @PathVariable("boxId") boxId: Long,
        @RequestBody newItems: MovingBoxExtras
    ): MovingBox {
        return movingBoxService.editExtras(sessionId, boxId, newItems)
    }
}