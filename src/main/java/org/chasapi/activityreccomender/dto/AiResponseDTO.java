package org.chasapi.activityreccomender.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder(toBuilder = true)
public record AiResponseDTO(
        @Size(max = 50) String summary,
        @Max(1)@Min(0) Double confidence,
        Boolean AI_Available,
        @NotNull
        List<@Pattern( regexp = "^(activity|commercial|catering|entertainment|heritage|leisure|natural|tourism|camping|beach|sport)$")
                String> recommendations
)
{ }
