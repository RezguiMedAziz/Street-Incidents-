//package Street.Incidents.Project.Street.Incidents.Project.entities.converter;
//
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.util.Base64;
//
//@Converter
//public class CryptoDoubleConverter implements AttributeConverter<Double, String> {
//
//    private static final String SECRET = "maCleSecrete1234"; // clé AES 16 caractères
//    private static final String ALGORITHM = "AES";
//
//    private SecretKeySpec getKey() {
//        return new SecretKeySpec(SECRET.getBytes(), ALGORITHM);
//    }
//
//    @Override
//    public String convertToDatabaseColumn(Double attribute) {
//        if (attribute == null) return null;
//        try {
//            Cipher cipher = Cipher.getInstance(ALGORITHM);
//            cipher.init(Cipher.ENCRYPT_MODE, getKey());
//            byte[] encrypted = cipher.doFinal(attribute.toString().getBytes());
//            return Base64.getEncoder().encodeToString(encrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("Erreur chiffrement", e);
//        }
//    }
//
//    @Override
//    public Double convertToEntityAttribute(String dbData) {
//        if (dbData == null) return null;
//        try {
//            Cipher cipher = Cipher.getInstance(ALGORITHM);
//            cipher.init(Cipher.DECRYPT_MODE, getKey());
//            byte[] decoded = Base64.getDecoder().decode(dbData);
//            String decrypted = new String(cipher.doFinal(decoded));
//            return Double.valueOf(decrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("Erreur déchiffrement", e);
//        }
//    }
//}
