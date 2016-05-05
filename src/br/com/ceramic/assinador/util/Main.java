package br.com.ceramic.assinador.util;

import com.fincatto.nfe310.NFeConfig;
import com.fincatto.nfe310.assinatura.AssinaturaDigital;
import com.fincatto.nfe310.classes.NFProtocolo;

import com.fincatto.nfe310.classes.lote.consulta.NFLoteConsultaRetorno;
import com.fincatto.nfe310.classes.lote.envio.NFLoteEnvio;
import com.fincatto.nfe310.classes.lote.envio.NFLoteEnvioRetorno;
import com.fincatto.nfe310.classes.lote.envio.NFLoteIndicadorProcessamento;
import com.fincatto.nfe310.classes.nota.NFNota;
import com.fincatto.nfe310.classes.nota.NFNotaProcessada;
import com.fincatto.nfe310.parsers.NotaParser;
import com.fincatto.nfe310.webservices.WSFacade;
import org.xml.sax.SAXException;
import javax.swing.*;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class Main extends JFrame implements Runnable {

    private Thread threadMonitor;
    private String dir;

    private Properties properties;

    private String pastaRaizMonitoramentoNota;

    private static final String raizUsuario = System.getProperty("user.home") + File.separatorChar;

    private String password;

    private String raizNFe;

    public static void main(String[] args) {
        new Main().init();
    }

    private void init() {
//5744

        try {
            properties = getProp();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.pastaRaizMonitoramentoNota = properties.getProperty("pastaAppMonitoramento");

        if(this.pastaRaizMonitoramentoNota.equals("undefined")){
            this.pastaRaizMonitoramentoNota = raizUsuario + "Google Drive";
        }

        // diretorio onde vai ser feito a verificacao
        setDir(this.pastaRaizMonitoramentoNota + File.separatorChar + ".nfe" + File.separatorChar);

        File diretorio = new File(dir);

        if(diretorio.isFile() == false){
            diretorio.mkdir();
        }

        File diretorioNaoAssinadas = new File(dir + File.separatorChar + "sign");
        if(diretorioNaoAssinadas.isFile() == false){
            diretorioNaoAssinadas.mkdir();
        }

        File diretorioAssinadas = new File(dir + File.separatorChar + "signed");
        if(diretorioAssinadas.isFile() == false){
            diretorioAssinadas.mkdir();
        }

        File diretorioEnviar = new File(dir + File.separatorChar + "send");
        if(diretorioEnviar.isFile() == false){
            diretorioEnviar.mkdir();
        }

        this.threadMonitor = new Thread(this);
        this.threadMonitor.start();
    }

    private String assinarXML(String xml, NFeConfig config){

            String xmlAssinado = "";

            int cont = 1;
            while (cont < 3){
                this.password = capturarPassword(cont);

                try {
                    xmlAssinado = new AssinaturaDigital(config).assinarDocumento(xml);
                    break;
                } catch (NoSuchAlgorithmException e) {
                    JOptionPane.showMessageDialog(this, "Falha ao buscar o algoritmo para checar o certificado! Contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (CertificateException e) {
                    JOptionPane.showMessageDialog(this, "Falha na leitura do certifica! Verifique o cartão!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e){
                    JOptionPane.showMessageDialog(this, "PIN errado! CUIDADO, isto pode causar bloqueio do certificado!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    continue;
                } catch (KeyStoreException e) {
                    JOptionPane.showMessageDialog(this, "Falha no KeyStore! Contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (XMLSignatureException e) {
                    JOptionPane.showMessageDialog(this, "Erro no formato do arquivo XML!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (MarshalException e) {
                    JOptionPane.showMessageDialog(this, "Erro na leitura do arquivo XML!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (TransformerException e) {
                    JOptionPane.showMessageDialog(this, "Erro ao transformar o XML em objeto!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (SAXException e) {
                    JOptionPane.showMessageDialog(this, e.getCause(), "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (UnrecoverableEntryException e) {
                    JOptionPane.showMessageDialog(this, "Problema no certificado!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    JOptionPane.showMessageDialog(this, "Problema de configuração! Se o problema persistir, contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    JOptionPane.showMessageDialog(this, "Algoritmo inválido! Se o problema persistir contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Falha ao ler o XML! Se o problema persistir contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                }

                cont ++;
            }

            if(cont == 3){
                JOptionPane.showMessageDialog(this, "Você fez 2 tentativas de senha errada! Confira a senha para realizar uma nova tentativa!", "Erro", JOptionPane.ERROR_MESSAGE);
            }

            JOptionPane.showMessageDialog(this, "NOTA ASSINADA COM SUCESSO!", "SUCESSO!", JOptionPane.INFORMATION_MESSAGE);


            return xmlAssinado;


    }

    public void salvarXMLEnviado(String xml, String arquivo){

        this.raizNFe = this.pastaRaizMonitoramentoNota + File.separatorChar + ".nfe";

        File diretorio = new File(this.raizNFe);

        if(diretorio.isFile() == false){
            criarEstruturaPastasNFe(diretorio);
        }

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(this.pastaRaizMonitoramentoNota + File.separatorChar + ".nfe" + File.separatorChar + "finalized" + File.separatorChar + arquivo), "UTF-8"
            ));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            out.write(xml);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private void verificarEnviarXML(String dir) {

        File diretorio = new File(dir + File.separatorChar + "send");

        File arquivos[] = diretorio.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".xml");
            }
        });

        for (int i = 0; i < arquivos.length; i++) {
            File file = arquivos[i];
            String nome = file.getName();
            NFLoteEnvioRetorno respostaEnvio = enviarXML(file);
            //salvarRespostaEnvio(respostaEnvio.getInfoRecebimento().getRecibo(), nome);
        }
    }

    private NFLoteEnvioRetorno enviarXML(File xmlEnviar){

        try {
            String xml = new String(Files.readAllBytes(xmlEnviar.toPath()));
            xmlEnviar.delete();

            String respostaEnvio = "";
            NFLoteEnvioRetorno retorno = null;

            int cont = 1;
            while (cont < 3){
                this.password = capturarPassword(cont);

                try {

                    final NFNota nota = new NotaParser().notaParaObjeto(xml);

                    NFLoteEnvio lote = new NFLoteEnvio();
                    lote.setIdLote("1");
                    lote.setIndicadorProcessamento(NFLoteIndicadorProcessamento.PROCESSAMENTO_ASSINCRONO);
                    lote.setVersao("3.10");
                    java.util.List<NFNota> notas =  new ArrayList<NFNota>();
                    notas.add(nota);

                    lote.setNotas(notas);

                    NfeConfig nFeConfig = new NfeConfig(this.password);
                    retorno = new WSFacade(nFeConfig).enviaLote(lote);

                    String recibo = retorno.getInfoRecebimento().getRecibo();
                    System.out.println(recibo);

                    NFLoteConsultaRetorno loteRecupetado = new WSFacade(nFeConfig).consultaLote(recibo);

                    for (NFProtocolo prot : loteRecupetado.getProtocolos()){

                        final String xmlNotaRecuperadaAssinada = assinarXML(xml, nFeConfig);
                        final NFNota notaRecuperadaAssinada = new NotaParser().notaParaObjeto(xmlNotaRecuperadaAssinada);
                        final NFNotaProcessada notaProcessada = new NFNotaProcessada();
                        notaProcessada.setVersao(new BigDecimal(NFeConfig.VERSAO_NFE));
                        notaProcessada.setProtocolo(prot);
                        notaProcessada.setNota(notaRecuperadaAssinada);
                        String xmlNotaProcessadaPeloSefaz = notaProcessada.toString();

                        System.out.println(prot.getProtocoloInfo().getChave());
                        salvarXMLEnviado(xmlNotaProcessadaPeloSefaz, prot.getProtocoloInfo().getChave());

                    }


                    //AssinaturaDigitalApplet assinador = new AssinaturaDigitalApplet(password);
                    //xmlAssinado = assinador.assinarDocumento(xml);
                    break;
                } catch (NoSuchAlgorithmException e) {
                    JOptionPane.showMessageDialog(this, "Falha ao buscar o algoritmo para checar o certificado! Contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (CertificateException e) {
                    JOptionPane.showMessageDialog(this, "Falha na leitura do certifica! Verifique o cartão!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e){
                    JOptionPane.showMessageDialog(this, "PIN errado! CUIDADO, isto pode causar bloqueio do certificado!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    continue;
                } catch (KeyStoreException e) {
                    JOptionPane.showMessageDialog(this, "Falha no KeyStore! Contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (XMLSignatureException e) {
                    JOptionPane.showMessageDialog(this, "Erro no formato do arquivo XML!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (MarshalException e) {
                    JOptionPane.showMessageDialog(this, "Erro na leitura do arquivo XML!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (TransformerException e) {
                    JOptionPane.showMessageDialog(this, "Erro ao transformar o XML em objeto!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (SAXException e) {
                    JOptionPane.showMessageDialog(this, e.getCause(), "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (UnrecoverableEntryException e) {
                    JOptionPane.showMessageDialog(this, "Problema no certificado!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    JOptionPane.showMessageDialog(this, "Problema de configuração! Se o problema persistir, contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    JOptionPane.showMessageDialog(this, "Algoritmo inválido! Se o problema persistir contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    JOptionPane.showMessageDialog(this, "Algoritmo inválido! Se o problema persistir contate o suporte!", "Erro", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                cont ++;
            }

            if(cont == 3){
                JOptionPane.showMessageDialog(this, "Você fez 2 tentativas de senha errada! Confira a senha para realizar uma nova tentativa!", "Erro", JOptionPane.ERROR_MESSAGE);
            }

            JOptionPane.showMessageDialog(this, "NOTA ENVIADA COM SUCESSO!", "SUCESSO!", JOptionPane.INFORMATION_MESSAGE);


            return retorno;



        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;


    }

    public void criarEstruturaPastasNFe(File diretorio){

        if(diretorio.isFile() == false){
            diretorio.mkdir();
        }

        File diretorioAssinadas = new File(this.raizNFe + File.separatorChar + "finalized");
        if(diretorioAssinadas.isFile() == false){
            diretorioAssinadas.mkdir();
        }

        File diretorioEnviar = new File(this.raizNFe + File.separatorChar + "send");
        if(diretorioEnviar.isFile() == false){
            diretorioEnviar.mkdir();
        }

    }

    public void newFile(String file) {
        System.out.println("evento -> novo arquivo encontrado!! " + file);
    }

    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();

        // thread que fica verificando a pasta
        while (this.threadMonitor == currentThread) {

            //verificarAssinarXML(getDir());
            verificarEnviarXML(getDir());

            try {
                Thread.sleep(500 * 5); // 10 segundos
            } catch (InterruptedException e) {
            }
        }
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public static Properties getProp() throws IOException {
        Properties props = new Properties();
        FileInputStream file = new FileInputStream(
                "./conf");
        props.load(file);
        return props;

    }



    private String capturarPassword(int tentativa){

        JFrame frame = new JFrame("Sistema de Notas");

        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 1, 2));
        label.add(new JLabel("Password", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 1, 2));
        JPasswordField password = new JPasswordField();
        controls.add(password);
        panel.add(controls, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(frame, panel, "INSIRA O PIN: "+tentativa+"ª tentativa!", JOptionPane.QUESTION_MESSAGE);

        return new String(password.getPassword());

    }


}
