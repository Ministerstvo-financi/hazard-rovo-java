package cz.mfcr.hazard.demo.config;

import cz.mfcr.hazard.demo.error.ClientError;
import cz.mfcr.hazard.rovo.v1.AisgPort;
import cz.mfcr.hazard.rovo.v1.ErrorMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.callback.CallbackHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Configuration
public class ClientConfig {
    @Autowired
    ClientProperties clientProperties;

    @Value("${outputXMLRequest:}")
    String requestLogFilePath;

    @Value("${outputXMLResponse:}")
    String responseLogFilePath;

    /**
     * Nastavení klienta AISG
     *
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    @Bean
    public AisgPort aisgClient() throws KeyStoreException, NoSuchAlgorithmException, IOException {

        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(AisgPort.class);
        jaxWsProxyFactoryBean.setAddress(clientProperties.getAddress());
        jaxWsProxyFactoryBean.getOutInterceptors().add(wss4jOut());
        jaxWsProxyFactoryBean.getOutInterceptors().add(loggingOutInterceptor());
        jaxWsProxyFactoryBean.getInInterceptors().add(loggingInInterceptor());
        jaxWsProxyFactoryBean.getInFaultInterceptors().add(loggingInInterceptor());
        jaxWsProxyFactoryBean.getInInterceptors().add(wss4jIn());
        AisgPort aisgPort = (AisgPort) jaxWsProxyFactoryBean.create();
        HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(aisgPort).getConduit();
        final TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setTrustManagers(createTrustManagers(clientProperties.getTrust()));
        httpConduit.setTlsClientParameters(tlsClientParameters);
        return aisgPort;
    }

    private TrustManager[] createTrustManagers(ClientProperties.KeystoreProperties trust) throws NoSuchAlgorithmException, KeyStoreException, IOException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(trust.getKeystoreType());

        Resource keystoreResource = new FileSystemResource(trust.getKeystoreLocation());
        if (!keystoreResource.getFile().exists()) {
            keystoreResource = new ClassPathResource(trust.getKeystoreLocation());
        }
        try (InputStream inputStream = keystoreResource.getInputStream()) {
            keyStore.load(inputStream, trust.getKeystorePassword().toCharArray());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ClientError("Nepodarilo se nakonfigurovat truststore", e);
        } catch (IOException e) {
            throw new ClientError("Nepodarilo se nacist soubor", e);
        }
    }

    /**
     * Logovani odchozích zpráv
     * dle parametru buď do souboru nebo na standardní výstup
     *
     * @return LoggingOutInterceptor
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException 
     */
    @Bean
    public LoggingOutInterceptor loggingOutInterceptor() throws FileNotFoundException, UnsupportedEncodingException {
        if (requestLogFilePath.isEmpty()) {
            return new LoggingOutInterceptor();
        } else {
            return new LoggingOutInterceptor(new PrintWriter(requestLogFilePath, "UTF-8"));
        }
    }

    /**
     * Logovani odpovědi
     * dle parametru buď do souboru nedo na standardní výstup
     *
     * @return LoggingInInterceptor
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException 
     */
    @Bean
    public LoggingInInterceptor loggingInInterceptor() throws FileNotFoundException, UnsupportedEncodingException {
        if (responseLogFilePath.isEmpty()) {
            return new LoggingInInterceptor();
        } else {
            return new LoggingInInterceptor(new PrintWriter(responseLogFilePath, "UTF-8"));
        }
    }

    /**
     * Kontrola podpisu odpovědi
     *
     * @return
     */
    public WSS4JInInterceptor wss4jIn() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        properties.put("trustProperties", getStoreProperties(clientProperties.getTrust()));
        properties.put(ConfigurationConstants.SIG_PROP_REF_ID, "trustProperties");
        return new WSS4JInInterceptor(properties);
    }

    /**
     * Podepisování odchozích zpráv
     *
     * @return WSS4JOutInterceptor
     */
    public WSS4JOutInterceptor wss4jOut() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        properties.put("signingProperties", getStoreProperties(clientProperties.getSign()));
        properties.put(ConfigurationConstants.SIG_PROP_REF_ID, "signingProperties");
        properties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        properties.put(ConfigurationConstants.USER, clientProperties.getSign().getKeyAlias());
        properties.put(ConfigurationConstants.SIGNATURE_PARTS,
                "{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body");
        properties.put(ConfigurationConstants.SIG_ALGO, SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        properties.put(ConfigurationConstants.SIG_DIGEST_ALGO, SignatureConstants.ALGO_ID_DIGEST_SHA256);
        properties.put(ConfigurationConstants.PW_CALLBACK_REF, (CallbackHandler) callbacks -> {
            for (javax.security.auth.callback.Callback callback : callbacks) {
                WSPasswordCallback pc = (WSPasswordCallback) callback;
                if (clientProperties.getSign().getKeyAlias().equals(pc.getIdentifier())) {
                    pc.setPassword(clientProperties.getSign().getKeyPassword());
                }
            }
        });
        return new WSS4JOutInterceptor(properties);
    }

    /**
     * Parametry pro podepisování/validaci
     *
     * @param props
     * @return
     */
    public Properties getStoreProperties(ClientProperties.KeystoreProperties props) {
        Properties properties = new Properties();
        properties.put("org.apache.wss4j.crypto.merlin.provider", "org.apache.wss4j.common.crypto.Merlin");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.type", props.getKeystoreType());
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", props.getKeystoreLocation());
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", props.getKeystorePassword());
        if (Objects.nonNull(props.getKeyAlias())) {
            properties.put("org.apache.wss4j.crypto.merlin.keystore.alias", props.getKeyAlias());
        }
        return properties;
    }

    @Bean
    ExitCodeExceptionMapper exitCodeToexceptionMapper() {
        return exception -> {

            Throwable rootCause = ExceptionUtils.getRootCause(exception);

            if (rootCause instanceof WSSecurityException) {
                return 2;
            }
            if (rootCause instanceof ErrorMessage) {
                return ((ErrorMessage) rootCause).getFaultInfo().getKod() - 9000 + 10;
            }
            return 1;
        };
    }
}
