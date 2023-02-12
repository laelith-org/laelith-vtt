package org.laelith.vtt.api

import org.laelith.vtt.domain.Info
import org.springframework.stereotype.Controller

@Controller
class DefaultApiImpl: DefaultApiService {
    override suspend fun info(): Info {
        return Info(
            version = "1.0.0",
            name = "Laelith VTT",
            description = "A virtual tabletop application for the laelith meta-verse project.",
            time = System.currentTimeMillis(),
        )
    }
}