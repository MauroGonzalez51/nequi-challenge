package co.com.bancolombia.model.franchise;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
// Required: ObjectMapperImp fills the model through setters when mapping entity -> model.
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Franchise {
    Long id;
    String name;
}
