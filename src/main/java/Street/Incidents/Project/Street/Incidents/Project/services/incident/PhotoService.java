package Street.Incidents.Project.Street.Incidents.Project.services.incident;

import Street.Incidents.Project.Street.Incidents.Project.entities.Incident;
import Street.Incidents.Project.Street.Incidents.Project.entities.Photo;
import Street.Incidents.Project.Street.Incidents.Project.repositories.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Value("${upload.path:uploads/incidents}")
    private String uploadPath;

    @Value("${upload.max-file-size:5242880}") // 5MB par défaut
    private long maxFileSize;

    @Value("${upload.max-files:5}") // Maximum 5 fichiers
    private int maxFiles;

    // Types MIME autorisés
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // Extensions autorisées
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    /**
     * Sauvegarde plusieurs photos avec validation stricte
     */
    public List<Photo> savePhotos(MultipartFile[] files, Incident incident) throws IOException {
        List<Photo> photos = new ArrayList<>();

        if (files == null || files.length == 0) {
            return photos;
        }

        // Validation du nombre de fichiers
        if (files.length > maxFiles) {
            throw new IllegalArgumentException(
                    "Nombre maximum de fichiers dépassé. Maximum autorisé : " + maxFiles
            );
        }

        // Créer le répertoire de stockage
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Valider le fichier
            validateImageFile(file);

            // Générer un nom unique et sécurisé
            String uniqueFilename = generateSecureFilename(file);
            Path filePath = uploadDir.resolve(uniqueFilename);

            // Vérifier que le chemin est sécurisé (pas de path traversal)
            if (!filePath.normalize().startsWith(uploadDir.normalize())) {
                throw new SecurityException("Tentative de path traversal détectée");
            }

            // Copier le fichier
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Créer l'entité Photo
            Photo photo = Photo.builder()
                    .nomFichier(sanitizeFilename(file.getOriginalFilename()))
                    .type(file.getContentType())
                    .taille(file.getSize())
                    .cheminStockage(filePath.toString())
                    .incident(incident)
                    .build();

            photos.add(photo);
        }

        return photoRepository.saveAll(photos);
    }

    /**
     * Validation complète d'un fichier image
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        // 1. Vérifier que le fichier n'est pas vide
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        // 2. Vérifier la taille
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("La taille du fichier dépasse la limite autorisée de %d MB",
                            maxFileSize / (1024 * 1024))
            );
        }

        // 3. Vérifier le Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé. Types acceptés : JPEG, PNG, GIF, WebP"
            );
        }

        // 4. Vérifier l'extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            throw new IllegalArgumentException(
                    "Extension de fichier non autorisée. Extensions acceptées : " +
                            String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        // 5. Validation du contenu réel (vérifier que c'est vraiment une image)
        validateImageContent(file.getInputStream());
    }

    /**
     * Vérifie que le fichier est réellement une image en lisant son contenu
     */
    private void validateImageContent(InputStream inputStream) throws IOException {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException(
                        "Le fichier n'est pas une image valide ou le format n'est pas supporté"
                );
            }

            // Vérifications supplémentaires sur les dimensions
            if (image.getWidth() > 10000 || image.getHeight() > 10000) {
                throw new IllegalArgumentException(
                        "Les dimensions de l'image sont trop grandes (max 10000x10000)"
                );
            }

            if (image.getWidth() < 10 || image.getHeight() < 10) {
                throw new IllegalArgumentException(
                        "Les dimensions de l'image sont trop petites (min 10x10)"
                );
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Erreur lors de la lecture de l'image : " + e.getMessage());
        }
    }

    /**
     * Vérifie l'extension du fichier
     */
    private boolean hasValidExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }

    /**
     * Génère un nom de fichier sécurisé et unique
     */
    private String generateSecureFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // Normaliser l'extension
            extension = extension.toLowerCase();
        }

        // UUID + timestamp pour unicité
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;
    }

    /**
     * Nettoie le nom de fichier original pour éviter les injections
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // Supprimer les caractères dangereux
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Supprime une photo du système
     */
    public void deletePhoto(Long photoId) throws IOException {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        Path filePath = Paths.get(photo.getCheminStockage());
        Files.deleteIfExists(filePath);
        photoRepository.delete(photo);
    }
}