package davobr.document.converter;

import java.util.Objects;

public record ConvertRequest(byte[] file, String inputFilter, String exportFilter) {
    public ConvertRequest {
        Objects.requireNonNull(file);
    }
}