package Street.Incidents.Project.Street.Incidents.Project.entities.Enums;

public enum Priorite {
        FAIBLE("Faible"),
        MOYENNE("Moyenne"),
        ELEVEE("Élevée"),
        CRITIQUE("Critique");

        private final String label;

    Priorite(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
