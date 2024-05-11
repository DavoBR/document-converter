package davobr.document.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.MediaType.*;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@RestController
public class ConverterController {
    private static final Logger LOG = LoggerFactory.getLogger(ConverterController.class);

    @Autowired
    private ConverterService converterService;
    
    @PostMapping(
        value = "/convert", 
        consumes = APPLICATION_JSON_VALUE,
        produces = {APPLICATION_JSON_VALUE, APPLICATION_PDF_VALUE, APPLICATION_OCTET_STREAM_VALUE}
    )
    @WithSpan()
    public ResponseEntity<Object> convertFromJson(
        @RequestBody ConvertRequest request,
        @RequestHeader(name = "Accept", required = false) @SpanAttribute() String accept
    ) throws ConverterException {
        LOG.info("Converting from JSON");
        var data = converterService.convert(
            request.file(), 
            request.inputFilter(), 
            request.exportFilter()
        );
        
        return createResponse(data, accept);
    }

    @PostMapping(
        value = "/convert", 
        consumes = MULTIPART_FORM_DATA_VALUE,
        produces = {APPLICATION_JSON_VALUE, APPLICATION_PDF_VALUE, APPLICATION_OCTET_STREAM_VALUE}
    )
    @WithSpan()
    public ResponseEntity<Object> convertFromForm(
        @RequestPart MultipartFile file,
        @RequestParam(required = false) @SpanAttribute() String inputFilter,
        @RequestParam(required = false) @SpanAttribute() String exportFilter,
        @RequestHeader(name = "Accept", required = false) String accept
    ) throws ConverterException, IOException {
        var data = converterService.convert(
            file.getBytes(), 
            inputFilter, 
            exportFilter
        );
        
        return createResponse(data, accept);
    }

    @PostMapping(
        value = "/convert", 
        consumes = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            APPLICATION_OCTET_STREAM_VALUE
        },
        produces = {APPLICATION_JSON_VALUE, APPLICATION_PDF_VALUE, APPLICATION_OCTET_STREAM_VALUE}
    )
    @WithSpan()
    public ResponseEntity<Object> convertFromRaw(
        @RequestBody byte[] file,
        @RequestParam(required = false) @SpanAttribute() String inputFilter,
        @RequestParam(required = false) @SpanAttribute() String exportFilter,
        @RequestHeader(name = "Accept", required = false) String accept
    ) throws ConverterException, IOException {
        var data = converterService.convert(
            file, 
            inputFilter, 
            exportFilter
        );
        
        return createResponse(data, accept);
    }

    @WithSpan()
    private ResponseEntity<Object> createResponse(byte[] data, @SpanAttribute() String accept) {
        LOG.info("Creating response");
        var response = ResponseEntity.ok();
       
        if (accept != null && accept.equals(APPLICATION_JSON_VALUE)) {
            return response.body(new ConvertResponse(data));
        }

        var text = new String(data);

        if(text.startsWith("%PDF")) {
            return response.contentType(APPLICATION_PDF).body(data);
        }

        return response.contentType(APPLICATION_OCTET_STREAM).body(data);
    }
}
