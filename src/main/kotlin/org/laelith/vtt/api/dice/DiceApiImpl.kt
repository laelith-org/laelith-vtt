package org.laelith.vtt.api.dice

import dev.diceroll.parser.ResultTree
import org.laelith.vtt.api.DiceApiService
import org.laelith.vtt.domain.DiceRollRequest
import org.laelith.vtt.domain.DiceRollResults
import org.springframework.stereotype.Controller
import dev.diceroll.parser.detailedRoll
import kotlinx.coroutines.flow.*
import org.laelith.vtt.domain.DiceRollResult

private fun getFlattenResults(resultTree: ResultTree): MutableList<DiceRollResult> {
    val results = mutableListOf<DiceRollResult>()
    resultTree.results.forEach {
        if (it.results.isEmpty()) {
            results.add(DiceRollResult(
                expression = it.expression.description(),
                result = it.value
            ))
        } else {
            results.addAll(getFlattenResults(it))
        }
    }

    return results
}

@Controller
class DiceApiImpl: DiceApiService {
    private val diceRollFlow = MutableSharedFlow<DiceRollResults>()
    override suspend fun roll(diceRollRequest: DiceRollRequest): DiceRollResults {
        val resultTree = detailedRoll(diceRollRequest.expression)

        // this.diceRollFlow.emit(result)
        return DiceRollResults(
            expression = resultTree.expression.description(),
            result = resultTree.value,
            rolls = getFlattenResults(resultTree)
        )
    }

    override fun rolls(): Flow<DiceRollResults> {
        return this.diceRollFlow
    }
}