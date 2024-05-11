package davobr.document.converter;

import java.util.Objects;

public record ConvertResponse(byte[] file) {
    public ConvertResponse {
        Objects.requireNonNull(file);
    }
}
