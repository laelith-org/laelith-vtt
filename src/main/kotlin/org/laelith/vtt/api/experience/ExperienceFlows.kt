package org.laelith.vtt.api.experience

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.laelith.vtt.domain.DiceRollResults
import org.laelith.vtt.domain.Experience

class ExperienceFlows(private val experience: Experience) {
    val experienceFlow: MutableStateFlow<Experience> = MutableStateFlow(experience)
    val rollsFlow: MutableSharedFlow<DiceRollResults> = MutableSharedFlow()
}