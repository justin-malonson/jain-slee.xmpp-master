/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jivesoftware.smack;

import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Trust manager that checks all certificates presented by the server. This class
 * is used during TLS negotiation. It is possible to disable/enable some or all checkings
 * by configuring the {@link ConnectionConfiguration}. The truststore file that contains
 * knows and trusted CA root certificates can also be configure in {@link ConnectionConfiguration}.
 *
 * @author Gaston Dombiak
 */
class ServerTrustManager implements X509TrustManager {

    private ConnectionConfiguration configuration;

    /**
     * Holds the domain of the remote server we are trying to connect
     */
    private String server;
    private KeyStore trustStore;

    public ServerTrustManager(String server, ConnectionConfiguration configuration) {
        this.configuration = configuration;
        this.server = server;

        try {
            trustStore = KeyStore.getInstance(configuration.getTruststoreType());
            trustStore.load(new FileInputStream(configuration.getTruststorePath()),
                    configuration.getTruststorePassword().toCharArray());
        }
        catch (Exception e) {
            e.printStackTrace();
            // Disable root CA checking
            configuration.setVerifyRootCAEnabled(false);
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String arg1)
            throws CertificateException {

        int nSize = x509Certificates.length;

        String peerIdentity = getPeerIdentity(x509Certificates[0]);

        if (configuration.isVerifyChainEnabled()) {
            // Working down the chain, for every certificate in the chain,
            // verify that the subject of the certificate is the issuer of the
            // next certificate in the chain.
            Principal principalLast = null;
            for (int i = nSize -1; i >= 0 ; i--) {
                X509Certificate x509certificate = x509Certificates[i];
                Principal principalIssuer = x509certificate.getIssuerDN();
                Principal principalSubject = x509certificate.getSubjectDN();
                if (principalLast != null) {
                    if (principalIssuer.equals(principalLast)) {
                        try {
                            PublicKey publickey =
                                    x509Certificates[i + 1].getPublicKey();
                            x509Certificates[i].verify(publickey);
                        }
                        catch (GeneralSecurityException generalsecurityexception) {
                            throw new CertificateException(
                                    "signature verification failed of " + peerIdentity);
                        }
                    }
                    else {
                        throw new CertificateException(
                                "subject/issuer verification failed of " + peerIdentity);
                    }
                }
                principalLast = principalSubject;
            }
        }

        if (configuration.isVerifyRootCAEnabled()) {
            // Verify that the the last certificate in the chain was issued
            // by a third-party that the client trusts.
            boolean trusted = false;
            try {
                trusted = trustStore.getCertificateAlias(x509Certificates[nSize - 1]) != null;
                if (!trusted && nSize == 1 && configuration.isSelfSignedCertificateEnabled())
                {
                    System.out.println("Accepting self-signed certificate of remote server: " +
                            peerIdentity);
                    trusted = true;
                }
            }
            catch (KeyStoreException e) {
                e.printStackTrace();
            }
            if (!trusted) {
                throw new CertificateException("root certificate not trusted of " + peerIdentity);
            }
        }

        if (configuration.isNotMatchingDomainCheckEnabled()) {
            // Verify that the first certificate in the chain corresponds to
            // the server we desire to authenticate.
            // Check if the certificate uses a wildcard indicating that subdomains are valid
            if (peerIdentity.startsWith("*.")) {
                // Remove the wildcard
                peerIdentity = peerIdentity.substring(2);
                // Check if the requested subdomain matches the certified domain
                if (!server.endsWith(peerIdentity)) {
                    throw new CertificateException("target verification failed of " + peerIdentity);
                }
            }
            else if (!server.equals(peerIdentity)) {
                throw new CertificateException("target verification failed of " + peerIdentity);
            }
        }

        if (configuration.isExpiredCertificatesCheckEnabled()) {
            // For every certificate in the chain, verify that the certificate
            // is valid at the current time.
            Date date = new Date();
            for (int i = 0; i < nSize; i++) {
                try {
                    x509Certificates[i].checkValidity(date);
                }
                catch (GeneralSecurityException generalsecurityexception) {
                    throw new CertificateException("invalid date of " + server);
                }
            }
        }

    }

    /**
     * Returns the identity of the remote server as defined in the specified certificate. The
     * identity is defined in the subjectDN of the certificate and it can also be defined in
     * the subjectAltName extension of type "xmpp". When the extension is being used then the
     * identity defined in the extension in going to be returned. Otherwise, the value stored in
     * the subjectDN is returned.
     *
     * @param x509Certificate the certificate the holds the identity of the remote server.
     * @return the identity of the remote server as defined in the specified certificate.
     */
    public static String getPeerIdentity(X509Certificate x509Certificate) {
        Principal principalSubject = x509Certificate.getSubjectDN();
        // TODO Look the identity in the subjectAltName extension if available
        String name = principalSubject.getName();
        if (name.startsWith("CN=")) {
            // Remove the CN= prefix
            name = name.substring(3);
        }
        return name;
    }

}
