package ru.svrd.stuff_moving_assistant.domain.moving_box

import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Service
import ru.svrd.stuff_moving_assistant.domain.exception.BaseException
import ru.svrd.stuff_moving_assistant.infrastructure.repository.MovingBoxRepository

@Service
class MovingBoxService(private val movingBoxRepository: MovingBoxRepository) {

    fun newBox(sessionId: Long, title: String): MovingBox {
        return movingBoxRepository.saveNewMovingBox(sessionId, title)
            ?: throw BaseException(
                message = "Cannot insert moving box with title $title",
                httpCode = INTERNAL_SERVER_ERROR
            )
    }

    fun editExtras(sessionId: Long, boxId: Long, newItems: MovingBoxExtras): MovingBox {
        return movingBoxRepository.editItems(sessionId, boxId, newItems)
            ?: throw BaseException(
                message = "Cannot update box with id $boxId",
                httpCode = INTERNAL_SERVER_ERROR,
            )
    }

    fun findBoxesInSession(sessionId: Long, item: String?): List<MovingBox> {
        val query = if (item != null) mapOf("item" to item) else mapOf()
        return movingBoxRepository.findBoxesBySessionId(sessionId, query)
    }
}
