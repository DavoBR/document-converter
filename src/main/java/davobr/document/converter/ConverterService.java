package davobr.document.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.SequenceInputStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@Service
public class ConverterService {
    private static final Logger LOG = LoggerFactory.getLogger(ConverterService.class);

    private static boolean initialized = false;
    
    @Value("${converter.defaultExportFilter:writer_pdf_Export}")
    private String defaultExportFilter;

    private XComponentContext xContext;
    private XMultiComponentFactory xMCF;
    private Object oDesktop;
    private XComponentLoader xCompLoader;

    /**
     * Initializes the converter service.
     *
     * @throws ConverterException If an error occurs during initialization.
     */
    @PostConstruct
    @WithSpan()
    public void init() throws ConverterException {
        if (initialized) {
            return;
        }

        LOG.info("Initializing");

        Path tempDir = null;

        try {
            LOG.info("Creating temporary directory");
            tempDir = Files.createTempDirectory("office_");
        } catch (IOException e) {
            throw new ConverterException("Failed to create temporary directory", e);
        }

        var cmdArgs = new String[] {
            "--headless",
            "--invisible",
            "--nocrashreport",
            "--nodefault",
            "--nofirststartwizard",
            "--nologo",
            "--norestore",
            String.format("-env:UserInstallation=file:///%s", tempDir.toString()),
        };

        try {
            LOG.info("Boostrapping LibreOffice");
            xContext = Bootstrap.bootstrap(cmdArgs);
            LOG.info("Connected to a running LibreOffice");
        } catch (BootstrapException e) {
            throw new ConverterException("Failed to bootstrap LibreOffice", e);
        }

        LOG.info("Getting LibreOffice services");
        // Getting the remote office service manager
        xMCF = xContext.getServiceManager();

        // Getting the remote office desktop
        try {
            LOG.info("Creating LibreOffice desktop");
            oDesktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
        } catch (Exception e) {
            throw new ConverterException("Failed to create LibreOffice desktop", e);
        }

        // Getting the XComponentLoader interface of the desktop
        xCompLoader = UnoRuntime.queryInterface(XComponentLoader.class, oDesktop);

        LOG.info("Initialized");

        initialized = true;
    }

    /**
     * Converts the input data from one format to another.
     *
     * @param inputData The input data to convert.
     * @param inputFilter The filter to use for the input data.
     * @param exportFilter The filter to use for the output data. If null, the default export filter is used.
     * @return The converted data.
     * @throws ConverterException If an error occurs during conversion.
     */
    @WithSpan()
    public byte[] convert(byte[] inputData, 
        @SpanAttribute() String inputFilter, 
        @SpanAttribute() String exportFilter) throws ConverterException {
            
        LOG.info("Converting file with length: " + inputData.length + " bytes");

        if (inputFilter != null) {
            LOG.info("Using input filter: " + inputFilter);
        } else {
            exportFilter = defaultExportFilter;
            LOG.info("Using default export filter: " + defaultExportFilter);
        }

        // https://help.libreoffice.org/latest/he/text/shared/guide/convertfilters.html
        if (exportFilter != null) {
            LOG.info("Using export filter: " + exportFilter);
        } else {
            exportFilter = defaultExportFilter;
            LOG.info("Using default export filter: " + defaultExportFilter);
        }

        // Creating sequence input stream from input data
        var inputStream = SequenceInputStream.createStreamFromSequence(xContext, inputData);

        // Define the properties for load document
        var inputPropsMap = new HashMap<String, Object>();
        inputPropsMap.put("InputStream", inputStream);
        if (inputFilter != null) {
            inputPropsMap.put("FilterName", inputFilter);
        }
        var inputProps = dictToProps(inputPropsMap);

        // Load the document
        XComponent oDocToStore;

        try {
            LOG.info("Loading document with filter: " + inputFilter);
            oDocToStore = xCompLoader.loadComponentFromURL("private:stream", "_default", 0, inputProps);
        } catch (IllegalArgumentException | com.sun.star.io.IOException e) {
            throw new ConverterException("Failed to load document", e);
        }

        // Define the properties for store document
        var xStorable = UnoRuntime.queryInterface(XStorable.class, oDocToStore);

        // Create a sequence output stream
        var outputStream = new XOutputStreamImpl();

        // Preparing properties for converting the document
        var outputProps = dictToProps(Map.of(
            "OutputStream", outputStream,
            "FilterName", exportFilter
        ));

        try {
            LOG.info("Converting the document with filter: " + exportFilter);
            xStorable.storeToURL("private:stream", outputProps);
        } catch (com.sun.star.io.IOException e) {
            throw new ConverterException("Failed to convert document", e);
        }

        return outputStream.toByteArray();
    }

    private static final PropertyValue[] dictToProps(Map<String, Object> dict) {
        var props = new PropertyValue[dict.size()];
        int i = 0;
        for (var entry : dict.entrySet()) {
            if(entry.getValue() == null) {
                continue;
            }
            props[i] = new PropertyValue();
            props[i].Name = entry.getKey();
            props[i].Value = entry.getValue();
            i++;
        }
        return props;
    }
}
