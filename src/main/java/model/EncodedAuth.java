package model;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EncodedAuth(
        String username,
        int iterations,
        String saltBase64,
        String hashBase64
) {};
