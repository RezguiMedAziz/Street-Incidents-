package Street.Incidents.Project.Street.Incidents.Project.entities.Enums;

public enum Departement {

    INFRASTRUCTURE("Infrastructure"),
    PROPRETE("Propreté"),
    SECURITE("Sécurité"),
    ECLAIRAGE("Éclairage"),
    SIGNALISATION("Signalisation");

    private final String label;

    Departement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // Affiche le label français automatiquement
    }
}
