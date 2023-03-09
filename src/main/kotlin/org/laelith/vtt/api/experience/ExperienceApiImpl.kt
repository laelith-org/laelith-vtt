package org.laelith.vtt.api.experience

import kotlinx.coroutines.flow.*
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
            players = mutableListOf(),
            state = Experience.State.created,
            gm = experienceIn.gm,
        )

        val experienceFlows = ExperienceFlows(experience)
        this.experienceFlowMap[experience.id] = experienceFlows

        this.experienceMapFlow.value.toMutableMap().also {
            it[experience.id] = experience
            this.experienceMapFlow.value = it
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

    override suspend fun joinExperience(id: String, experienceInfoIn: ExperienceInfoIn) {
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.players.add(experienceInfoIn.player)
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun listExperience(): Flow<MutableList<Experience>> {
        return this.experienceMapFlow.map { experienceMap ->
            experienceMap.values.toMutableList() }
    }

    override suspend fun quitExperience(id: String, experienceInfoIn: ExperienceInfoIn) {
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.players.remove(experienceInfoIn.player)
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