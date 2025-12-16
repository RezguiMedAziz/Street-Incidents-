package Street.Incidents.Project.Street.Incidents.Project.services.incident;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Photo;
import Street.Incidents.Project.Street.Incidents.Project.repositories.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    // Chemin de stockage des photos (configurable dans application.properties)
    @Value("${upload.path:uploads/incidents}")
    private String uploadPath;

    /**
     * Sauvegarde plusieurs photos pour un incident
     */
    public List<Photo> savePhotos(MultipartFile[] files, Incident incident) throws IOException {
        List<Photo> photos = new ArrayList<>();

        if (files == null || files.length == 0) {
            return photos;
        }

        // Créer le répertoire de stockage s'il n'existe pas
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validation du type de fichier
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Seuls les fichiers image sont acceptés");
            }

            // Validation de la taille (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("La taille de l'image ne doit pas dépasser 5MB");
            }

            // Générer un nom de fichier unique
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Chemin complet du fichier
            Path filePath = uploadDir.resolve(uniqueFilename);

            // Copier le fichier
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Créer l'entité Photo
            Photo photo = Photo.builder()
                    .nomFichier(originalFilename)
                    .type(contentType)
                    .taille(file.getSize())
                    .cheminStockage(filePath.toString())
                    .incident(incident)
                    .build();

            photos.add(photo);
        }

        // Sauvegarder toutes les photos
        return photoRepository.saveAll(photos);
    }

    /**
     * Supprime une photo du système de fichiers et de la base de données
     */
    public void deletePhoto(Long photoId) throws IOException {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        // Supprimer le fichier physique
        Path filePath = Paths.get(photo.getCheminStockage());
        Files.deleteIfExists(filePath);

        // Supprimer de la base de données
        photoRepository.delete(photo);
    }
}