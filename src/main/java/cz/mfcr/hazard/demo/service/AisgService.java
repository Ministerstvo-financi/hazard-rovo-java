package cz.mfcr.hazard.demo.service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.mfcr.hazard.demo.config.ClientProperties;
import cz.mfcr.hazard.demo.error.ClientError;
import cz.mfcr.hazard.rovo.v1.AisgPort;
import cz.mfcr.hazard.rovo.v1.BaseRequestType;
import cz.mfcr.hazard.rovo.v1.BaseResponseType;
import cz.mfcr.hazard.rovo.v1.ErrorMessage;
import cz.mfcr.hazard.rovo.v1.OveritOsobuRequest;
import cz.mfcr.hazard.rovo.v1.OveritOsobyHromadneRequest;
import cz.mfcr.hazard.rovo.v1.ZiskatVysledkyOveritOsobyHromadneRequest;
import cz.mfcr.hazard.rovo.v1.ZmenitUdajeOsobyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

@Service
public class AisgService {
    @Autowired
    private ClientProperties clientProperties;

    @Autowired
    private AisgPort aisgClient;

    @Value("${outputJSONRequest:}")
    private String logJSONRequestPath;

    @Value("${outputJSONResponse:}")
    private String logJSONResponsePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws ErrorMessage;
    }

    @PostConstruct
    private void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }

    /**
     * Zavolání klienta a zalogování operace
     *
     * @param data   vlastní data požadavku
     * @param client odkaz na metodu klienta {@link AisgPort}
     * @param <T> request
     * @param <R> response
     */
    private <T extends BaseRequestType, R extends BaseResponseType> R callAisg(T data, CheckedFunction<T, R> client) {
        setCisloICO(data);
        try {
            logRequestJSON(data);
            R response = client.apply(data);
            logResponseJSON(response);
            return response;
        } catch (ErrorMessage errorMessage) {
            writeObject(errorMessage.getFaultInfo(), "ErroMessage");
            writeObject(errorMessage, "Exception");
            throw new ClientError(errorMessage);
        } catch (IOException e) {
            throw new ClientError(e);
        }
    }

    /**
     * Dotažení dat z JSON vstupního souboru
     *
     * @param type   typ dat
     * @param input  soubor s daty
     * @param client odkaz na metodu klienta
     * @param <T> request
     * @param <R> response
     * @throws IOException
     */
    @SuppressWarnings("UnusedReturnValue")
    private <T extends BaseRequestType, R extends BaseResponseType> R callAisg(Class<T> type, InputStream input, CheckedFunction<T, R> client) throws IOException {
        try {
            T data = readObject(input, type);
            return callAisg(data, client);
        } finally {
            input.close();
        }

    }

    public void test() {
        callAisg(new BaseRequestType(), aisgClient::test);
    }

    public void overitOsobu(String inputFilePath) throws IOException {
        callAisg(
                OveritOsobuRequest.class,
                getInput("OveritOsobu.json", inputFilePath),
                aisgClient::overitOsobu);
    }

    public void zmenitUdajeOsoby(String inputFilePath) throws IOException {
        callAisg(
                ZmenitUdajeOsobyRequest.class,
                getInput("ZmenitUdajeOsoby.json", inputFilePath),
                aisgClient::zmenitUdajeOsoby);
    }

    public void overitOsobyHromadne(String inputFilePath) throws IOException {
        callAisg(OveritOsobyHromadneRequest.class, getInput("OveritOsobyHromadne.json", inputFilePath), aisgClient::overitOsobyHromadne);
    }

    public void ziskatVysledkyOveritOsobyHromadne(String inputFilePath) throws IOException {
        callAisg(ZiskatVysledkyOveritOsobyHromadneRequest.class, getInput("ZiskatVysledkyOveritOsobyHromadne.json", inputFilePath), aisgClient::ziskatVysledkyOveritOsobyHromadne);
    }

    private void setCisloICO(final BaseRequestType request) {
        request.setCisloPozadavku(UUID.randomUUID().toString());
        request.setICOVCP(clientProperties.getIcovcp());
    }

    private InputStream getInput(String defaultName, String filePath) {
        try {
            return filePath == null ? new ClassPathResource("/data/" + defaultName, this.getClass()).getInputStream() : new FileInputStream(filePath);
        } catch (IOException e) {
            throw new ClientError("Nepodařilo se načíst soubor", e);
        }
    }

    private <T> T readObject(InputStream input, Class<T> type) {
        try {
            return objectMapper.readValue(input, type);
        } catch (IOException e) {
            throw new ClientError("Nepodařilo se načíst data", e);
        }
    }

    private void logRequestJSON(Object o) throws IOException {
        if (logJSONRequestPath.isEmpty()) {
            System.out.println("Request:");
            System.out.println(objectMapper.writeValueAsString(o));
        } else {
            PrintWriter printWriter = new PrintWriter(logJSONRequestPath, "UTF-8");
            objectMapper.writeValue(printWriter, o);
            printWriter.close();
        }
    }

    private void logResponseJSON(Object o) throws IOException {
        if (logJSONResponsePath.isEmpty()) {
            System.out.println("Response:");
            System.out.println(objectMapper.writeValueAsString(o));
        } else {
            PrintWriter printWriter = new PrintWriter(logJSONResponsePath, "UTF-8");
            objectMapper.writeValue(printWriter, o);
            printWriter.close();
        }
    }

    private void writeObject(Object o, String label) {
        System.out.println(label);
        try {
            System.out.println(objectMapper.writeValueAsString(o));
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
