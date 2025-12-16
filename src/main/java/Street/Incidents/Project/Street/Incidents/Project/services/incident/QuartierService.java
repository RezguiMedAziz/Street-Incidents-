package Street.Incidents.Project.Street.Incidents.Project.services.incident;

import Street.Incidents.Project.Street.Incidents.Project.entities.Quartier;
import Street.Incidents.Project.Street.Incidents.Project.repositories.QuartierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuartierService {

    @Autowired
    private QuartierRepository quartierRepository;

    /**
     * Trouve ou crée un quartier basé sur les informations géographiques
     * @param gouvernorat Gouvernorat
     * @param municipalite Municipalité (contient les données de délégation)
     * @return Le quartier trouvé ou créé
     */
    @Transactional
    public Quartier findOrCreateQuartier(String gouvernorat, String municipalite) {

        // Si aucune information géographique n'est fournie, retourner null
        if (isAllEmpty(gouvernorat, municipalite)) {
            return null;
        }

        // Chercher si le quartier existe déjà
        return quartierRepository
                .findByGouvernoratAndMunicipalite(gouvernorat, municipalite)
                .orElseGet(() -> {
                    // Le quartier n'existe pas, le créer
                    Quartier newQuartier = Quartier.builder()
                            .gouvernorat(gouvernorat)
                            .municipalite(municipalite)
                            .build();

                    // Sauvegarder et retourner le nouveau quartier
                    return quartierRepository.save(newQuartier);
                });
    }

    /**
     * Vérifie si toutes les chaînes sont vides ou null
     */
    private boolean isAllEmpty(String... strings) {
        for (String str : strings) {
            if (str != null && !str.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sauvegarde un quartier
     */
    @Transactional
    public Quartier saveQuartier(Quartier quartier) {
        return quartierRepository.save(quartier);
    }
}