package com.example.fraud.dto;

import com.example.fraud.entity.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Request/response shapes for customers. */
public final class CustomerDto {

    private CustomerDto() {
    }

    public record Request(
            @NotBlank(message = "fullName is required") @Size(max = 150) String fullName,
            @NotBlank(message = "email is required") @Email(message = "email must be a valid address")
            @Size(max = 254) String email,
            @NotBlank(message = "homeCountry is required")
            @Pattern(regexp = "[A-Z]{2}", message = "homeCountry must be a 2-letter ISO code such as ZA")
            String homeCountry) {
    }

    public record Response(Long id, String fullName, String email, String homeCountry, OffsetDateTime createdAt) {

        public static Response from(Customer c) {
            return new Response(c.getId(), c.getFullName(), c.getEmail(), c.getHomeCountry(),
                    c.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
    }
}
