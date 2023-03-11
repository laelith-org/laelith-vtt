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
    private val diceService: DiceApiService,
    private val experienceMapper: ExperienceMapper,
) : ExperienceApiService {
    private val experienceMapFlow: MutableStateFlow<MutableMap<String, ExperienceInfo>> = MutableStateFlow(mutableMapOf())
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
        this.experienceMapFlow.emit(this.experienceMapFlow.value.toMutableMap().also {
            it[experience.id] = experienceMapper.toInfo(experience)
        });

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
        return this.experienceFlowMap[id]?.experienceFlow?.value
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
    }

    override suspend fun joinExperience(id: String, player: Player) {
        logger.info("Joining experience $id.")
        val experience = this.experienceFlowMap[id]?.experienceFlow?.value?.copy()
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
        experience.players = experience.players.toMutableList().also {
            if (!it.contains(player)) {
                it.add(player)
            }
        }
        this.experienceFlowMap[id]?.experienceFlow?.emit(experience)
    }

    override fun listExperience(): Flow<MutableList<ExperienceInfo>> {
        logger.info("Listing experiences stream.")
        return this.experienceMapFlow.map { it.values.toMutableList() }
    }

    override suspend fun quitExperience(id: String, player: Player) {
        logger.info("Quitting experience $id.")
        val experience = this.experienceFlowMap[id]?.experienceFlow?.value?.copy()
            ?: throw ExperienceNotFoundException("Experience with id $id not found.")
        experience.players = experience.players.toMutableList().also { it.remove(player) }
        this.experienceFlowMap[id]?.experienceFlow?.emit(experience)
    }

    override suspend fun removeExperience(id: String) {
        logger.info("Removing experience $id.")
        this.experienceFlowMap[id]?.experienceFlow?.value = this.experienceFlowMap[id]?.experienceFlow?.value?.copy(state = Experience.State.deleted)
                ?: throw ExperienceNotFoundException("Experience with id $id not found.")

        this.experienceMapFlow.emit(this.experienceMapFlow.value.toMutableMap().also {
            it.remove(id)
        });

        this.experienceFlowMap.remove(id)
        logger.info("Experience $id removed.")
    }
}