package Street.Incidents.Project.Street.Incidents.Project.repositories;

import Street.Incidents.Project.Street.Incidents.Project.entities.Rapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {

    // Trouver tous les rapports d'un utilisateur
    List<Rapport> findByGenerePar_Id(Long userId);

    // Trouver les rapports par type
    List<Rapport> findByType(String type);
}