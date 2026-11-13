package co.com.bancolombia.api.dto.request;

import jakarta.validation.constraints.NotNull;

/** Shape of the request: required fields must be present. Values are checked in the domain. */
public record AddBranchRequest(@NotNull(message = RequestMessages.REQUIRED) String name) {

}
