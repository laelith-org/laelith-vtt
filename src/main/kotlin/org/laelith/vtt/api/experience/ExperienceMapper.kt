package org.laelith.vtt.api.experience

import org.laelith.vtt.domain.Experience
import org.laelith.vtt.domain.ExperienceInfo
import org.springframework.stereotype.Service

@Service
class ExperienceMapper {
    fun toInfo(experience: Experience): ExperienceInfo {
        return ExperienceInfo(
            id = experience.id,
            name = experience.name,
            gm = experience.gm,
        )
    }
}