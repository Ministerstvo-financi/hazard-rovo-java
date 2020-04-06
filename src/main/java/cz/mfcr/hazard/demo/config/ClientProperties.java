package cz.mfcr.hazard.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client")
public class ClientProperties {
    private String address;
    private String icovcp;

    public String getIcovcp() {
        return icovcp;
    }

    public void setIcovcp(String icovcp) {
        this.icovcp = icovcp;
    }

    private KeystoreProperties sign = new KeystoreProperties();

    private KeystoreProperties trust = new KeystoreProperties();

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public KeystoreProperties getSign() {
        return sign;
    }

    public void setSign(KeystoreProperties sign) {
        this.sign = sign;
    }

    public KeystoreProperties getTrust() {
        return trust;
    }

    public void setTrust(KeystoreProperties trust) {
        this.trust = trust;
    }

    public static class KeystoreProperties {
        private String keystoreLocation;
        private String keystoreType;
        private String keyAlias;
        private String keyPassword;
        private String keystorePassword;

        public String getKeystoreLocation() {
            return keystoreLocation;
        }

        public void setKeystoreLocation(String keystoreLocation) {
            this.keystoreLocation = keystoreLocation;
        }

        public String getKeystoreType() {
            return keystoreType;
        }

        public void setKeystoreType(String keystoreType) {
            this.keystoreType = keystoreType;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }

        public void setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }
    }
}
