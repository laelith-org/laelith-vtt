package org.laelith.vtt.api.experience

import kotlinx.coroutines.flow.*
import org.hashids.Hashids
import org.laelith.vtt.api.DiceApiService
import org.laelith.vtt.api.ExperienceApiService
import org.laelith.vtt.api.exception.ExperienceNotFoundException
import org.laelith.vtt.domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller

@Controller
class ExperienceApiImpl (
    private val diceService: DiceApiService
) : ExperienceApiService {
    private val experienceMapFlow = MutableStateFlow<MutableMap<String, Experience>>(mutableMapOf())
    private val experienceFlowMap: MutableMap<String, ExperienceFlows> = mutableMapOf()
    private val logger: Logger = LoggerFactory.getLogger(ExperienceApiImpl::class.java)
    override suspend fun addExperience(experienceIn: ExperienceIn): Experience {
        logger.info("Adding experience ${experienceIn.name}.")
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

        logger.info("Experience ${experienceIn.name} added.")
        return experience
    }

    override fun experienceEventRolls(id: String): Flow<DiceRollResults> {
        logger.info("Getting experience event rolls stream for experience $id.")
        return this.experienceFlowMap[id]?.rollsFlow
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun experienceEvents(id: String): Flow<Experience> {
        logger.info("Getting experience events stream for experience $id.")
        return this.experienceFlowMap[id]?.experienceFlow
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun experienceRoll(id: String, diceRollRequest: DiceRollRequest) {
        logger.info("Rolling dice expression ${diceRollRequest.expression} for experience $id.")
        val result = this.diceService.roll(diceRollRequest)
        this.experienceFlowMap[id]?.rollsFlow?.emit(result)
    }

    override suspend fun getExperience(id: String): Experience {
        logger.info("Getting experience $id.")
        return this.experienceMapFlow.value[id]
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun joinExperience(id: String, experienceInfoIn: ExperienceInfoIn) {
        logger.info("Joining experience $id.")
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.players.add(experienceInfoIn.player)
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override fun listExperience(): Flow<MutableList<Experience>> {
        logger.info("Listing experiences stream.")
        logger.info("Experience list: ${this.experienceMapFlow.value}")
        return this.experienceMapFlow.map { experienceMap ->
            experienceMap.values.toMutableList() }
    }

    override suspend fun quitExperience(id: String, experienceInfoIn: ExperienceInfoIn) {
        logger.info("Quitting experience $id.")
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.players.remove(experienceInfoIn.player)
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun removeExperience(id: String) {
        logger.info("Removing experience $id.")
        this.experienceFlowMap[id]?.experienceFlow?.update { experience ->
            experience.state = Experience.State.deleted
            experience
        } ?: throw ExperienceNotFoundException("Experience with id $id not found.")

        this.experienceMapFlow.value.toMutableMap().also {
            it.remove(id)
            this.experienceMapFlow.value = it
        }

        logger.info("Experience $id removed.")
        this.experienceFlowMap.remove(id)
    }
}