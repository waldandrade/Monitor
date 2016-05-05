package com.fincatto.nfe310.assinatura;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AssinaturaDigitalApplet {
    private static final String C14N_TRANSFORM_METHOD = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    private static final String[] ELEMENTOS_ASSINAVEIS = new String[] { "infEvento", "infCanc", "infNFe", "infInut" };
    /**
     * final KeyStore keyStore = KeyStore.getInstance("PKCS12");
     */
    private final KeyStore keyStore;
    private final String password;

    public AssinaturaDigitalApplet(final KeyStore keyStore, String password) {
        this.keyStore = keyStore;
        this.password = password;
    }


    
    /**
     *
     * @param file
     * @param password
     * @throws Exception
     *
     * Cerrega o certificado do tipo A1 utilizando um arquivo JKS
     */
    public AssinaturaDigitalApplet(final File file, String password) throws Exception{

        this.keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream certificadoStream = new FileInputStream(file)) {
            keyStore.load(certificadoStream, password.toCharArray());
        }
        this.password = password;
    }
    
    /**
     * 
     * @param password
     * Exclusivo para certificado A3
     */
    public AssinaturaDigitalApplet(String password) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException{

        this.password = password;  

    	this.keyStore = KeyStore.getInstance("PKCS11");  
    	keyStore.load(null, this.password.toCharArray());     
    	
    }


    public boolean isValida(final InputStream xmlStream) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        final Document document = dbf.newDocumentBuilder().parse(xmlStream);
        final NodeList nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nodeList.getLength() == 0) {
            throw new Exception("Não foi encontrada a assinatura do XML.");
        }

        final String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        final XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
        final DOMValidateContext validateContext = new DOMValidateContext(new X509KeySelector(), nodeList.item(0));

        for (final String tag : AssinaturaDigitalApplet.ELEMENTOS_ASSINAVEIS) {
            final NodeList elements = document.getElementsByTagName(tag);
            if (elements.getLength() > 0) {
                validateContext.setIdAttributeNS((Element) elements.item(0), null, "Id");
            }
        }

        return signatureFactory.unmarshalXMLSignature(validateContext).validate(validateContext);
    }

    public String assinarDocumento(final String conteudoXml) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, ParserConfigurationException, IOException, SAXException, KeyStoreException, UnrecoverableEntryException, MarshalException, XMLSignatureException, TransformerException {
        

        final KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyStore.aliases().nextElement(), new KeyStore.PasswordProtection(this.password.toCharArray()));
        final XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");

        final List<Transform> transforms = new ArrayList<>(2);
        transforms.add(signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(signatureFactory.newTransform(AssinaturaDigitalApplet.C14N_TRANSFORM_METHOD, (TransformParameterSpec) null));

        final KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
        final X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList((X509Certificate) keyEntry.getCertificate()));
        final KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        final Document document = documentBuilderFactory.newDocumentBuilder().parse(IOUtils.toInputStream(conteudoXml));

        for (final String elementoAssinavel : AssinaturaDigitalApplet.ELEMENTOS_ASSINAVEIS) {
            final NodeList elements = document.getElementsByTagName(elementoAssinavel);
            for (int i = 0; i < elements.getLength(); i++) {
                final Element element = (Element) elements.item(i);
                final String id = element.getAttribute("Id");
                element.setIdAttribute("Id", true);

                final Reference reference = signatureFactory.newReference("#" + id, signatureFactory.newDigestMethod(DigestMethod.SHA1, null), transforms, null, null);
                final SignedInfo signedInfo = signatureFactory.newSignedInfo(signatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(reference));

                final XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
                signature.sign(new DOMSignContext(keyEntry.getPrivateKey(), element.getParentNode()));
            }
        }
        return this.converteDocumentParaXml(document);
    }

    private String converteDocumentParaXml(final Document document) throws TransformerFactoryConfigurationError, TransformerException, IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
            return outputStream.toString(Charsets.UTF_8.name());
        }
    }
}