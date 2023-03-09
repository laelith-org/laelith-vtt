package org.laelith.vtt.api.experience

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
    private val experienceMapFlow = MutableStateFlow<MutableMap<String, Experience>>(mutableMapOf())
    private val experienceFlowMap: MutableMap<String, ExperienceFlows> = mutableMapOf()
    override suspend fun addExperience(experienceIn: ExperienceIn): Experience {
        val hashId = Hashids("Laelith VTT Experience Salt")
        val experience = Experience(
            id = hashId.encode(System.currentTimeMillis()),
            name = experienceIn.name,
            users = mutableListOf(),
            state = Experience.State.created,
        )

        val experienceFlows = ExperienceFlows(experience)
        this.experienceFlowMap[experience.id] = experienceFlows

        this.experienceMapFlow.update { experienceMap ->
            experienceMap[experience.id] = experience
            experienceMap
        }

        return experience
    }

    override fun experienceEventRolls(id: String): Flow<DiceRollResults> {
        return this.experienceFlowMap[id]?.rollsFlow
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun experienceEvents(id: String): Flow<Experience> {
        return this.experienceFlowMap[id]?.experienceFlow
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun experienceRoll(id: String, diceRollRequest: DiceRollRequest) {
        val result = this.diceService.roll(diceRollRequest)
        this.experienceFlowMap[id]?.rollsFlow?.emit(result)
    }

    override suspend fun getExperience(id: String): Experience {
        return this.experienceMapFlow.value[id]
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun joinExperience(id: String) {
        val currentUserId = ""
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.users.add(currentUserId)
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun listExperience(): Flow<MutableMap<String, Experience>> {
        return this.experienceMapFlow
    }

    override suspend fun quitExperience(id: String) {
        val currentUserId = ""
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.users.remove(currentUserId)
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun removeExperience(id: String) {
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.state = Experience.State.deleted
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")

        this.experienceMapFlow.update { experienceMap ->
            experienceMap.remove(id)
            experienceMap
        }

        this.experienceFlowMap.remove(id)
    }
}