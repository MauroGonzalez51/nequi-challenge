package co.com.bancolombia.r2dbc.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("franchise")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseEntity {
    @Id
    private Long id;

    private String name;
}