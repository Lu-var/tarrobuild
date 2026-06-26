package cl.tarrobuild.compatibility.dto;

import java.util.List;

public record ProductDTO(
        Long id,
        String name,
        String categoryName,
        List<AttributeDTO> attributes
) {

    /**
     * Returns the value of the first attribute whose name matches the given name,
     * using a case-insensitive comparison. Returns {@code null} if not found.
     */
    public String getAttributeValue(String name) {
        if (attributes == null || name == null) {
            return null;
        }
        return attributes.stream()
                .filter(a -> name.equalsIgnoreCase(a.attributeName()))
                .map(AttributeDTO::attributeValue)
                .findFirst()
                .orElse(null);
    }
}
