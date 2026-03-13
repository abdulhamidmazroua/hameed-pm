package model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EncodedAuth(
        @JsonProperty("username") String username,
        @JsonProperty("iterations") int iterations,
        @JsonProperty("salt_base64") String saltBase64,
        @JsonProperty("hash_base64") String hashBase64
) {};
