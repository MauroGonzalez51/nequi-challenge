package co.com.bancolombia.model.franchise;

import java.util.List;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import co.com.bancolombia.model.branch.Branch;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Franchise {
    Long id;
    String name;
    List<Branch> branches;
}
