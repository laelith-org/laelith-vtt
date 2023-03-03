package org.laelith.vtt.api.dice

import dev.diceroll.parser.ResultTree
import org.laelith.vtt.api.DiceApiService
import org.laelith.vtt.domain.DiceRollRequest
import org.laelith.vtt.domain.DiceRollResults
import org.springframework.stereotype.Controller
import dev.diceroll.parser.detailedRoll
import org.laelith.vtt.domain.DiceRollResult


@Controller
class DiceApiImpl: DiceApiService {
    override suspend fun roll(diceRollRequest: DiceRollRequest): DiceRollResults {
        val resultTree = detailedRoll(diceRollRequest.expression)
        return DiceRollResults(
            expression = resultTree.expression.description(),
            result = resultTree.value,
            rolls = this.getFlattenResults(resultTree)
        )
    }

    private fun getFlattenResults(resultTree: ResultTree): List<DiceRollResult> {
        val results = mutableListOf<DiceRollResult>()
        resultTree.results.forEach {
            if (it.results.isEmpty()) {
                results.add(DiceRollResult(
                    expression = it.expression.description(),
                    result = it.value
                ))
            } else {
                results.addAll(this.getFlattenResults(it))
            }
        }

        return results
    }
}