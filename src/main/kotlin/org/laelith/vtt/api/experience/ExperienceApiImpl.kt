package org.laelith.vtt.api.experience

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.hashids.Hashids
import org.laelith.vtt.api.DiceApiService
import org.laelith.vtt.api.ExperienceApiService
import org.laelith.vtt.api.exception.ExperienceNotFoundException
import org.laelith.vtt.domain.*
import org.springframework.stereotype.Controller

@Controller
class ExperienceApiImpl (
    private val diceService: DiceApiService
) : ExperienceApiService {
    private val experienceMap: MutableMap<String, Experience> = mutableMapOf()
    private val experienceList: MutableList<Experience> = mutableListOf()
    private val experienceListFlow = MutableStateFlow<List<Experience>>(listOf())
    private val experienceFlowMap: MutableMap<String, ExperienceFlows> = mutableMapOf()
    override suspend fun addExperience(experienceIn: ExperienceIn): Experience {
        val hashId = Hashids("Laelith VTT Experience Salt")
        val experience = Experience(
            id = hashId.encode(System.currentTimeMillis()),
            name = experienceIn.name,
        )

        val experienceFlows = ExperienceFlows(experience)
        this.experienceFlowMap[experience.id] = experienceFlows

        this.experienceList.add(experience)
        this.experienceMap[experience.id] = experience
        this.experienceListFlow.emit(this.experienceList.toList())
        return experience
    }

    override fun experienceEventRolls(id: String): Flow<DiceRollResults> {
        return this.experienceFlowMap[id]?.rollsFlow
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun experienceRoll(id: String, diceRollRequest: DiceRollRequest) {
        val result = this.diceService.roll(diceRollRequest)
        this.experienceFlowMap[id]?.rollsFlow?.emit(result)
    }

    override suspend fun getExperience(id: String): Experience {
        return this.experienceMap[id]
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun joinExperience(id: String): Flow<ExperienceInfo> {
        TODO("Not yet implemented")
    }

    override fun listExperience(): Flow<List<Experience>> {
        return this.experienceListFlow
    }

    override suspend fun quitExperience(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun removeExperience(id: String): Experience {
        TODO("Not yet implemented")
    }
}