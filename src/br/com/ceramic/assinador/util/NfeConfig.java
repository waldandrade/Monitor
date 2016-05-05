package br.com.ceramic.assinador.util;

import com.fincatto.nfe310.NFeConfig;
import com.fincatto.nfe310.classes.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Waldney on 04/05/2016.
 */
public class NfeConfig implements NFeConfig{

    private String password = "changeit";

    public NfeConfig(String password){
        this.password = password;
    }

    @Override
    public NFTipoCertificado getTipoCertificado() {
        // TODO Auto-generated method stub
        return NFTipoCertificado.A3;
    }

    @Override
    public NFTipoCertificadoA3 getTipoCertificadoA3() {
        // TODO Auto-generated method stub
        return NFTipoCertificadoA3.SMARTCARD;
    }

    @Override
    public NFAmbiente getAmbiente() {
        // TODO Auto-generated method stub
        return NFAmbiente.HOMOLOGACAO;
    }

    @Override
    public byte[] getCertificado() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getCadeiaCertificados() throws IOException {
        // TODO Auto-generated method stub
        return Files.readAllBytes(Paths.get(new File("NFeCacerts").getPath()));
    }

    @Override
    public String getCertificadoSenha() {
        // TODO Auto-generated method stub
        return this.password;
    }

    @Override
    public String getCadeiaCertificadosSenha() {
        // TODO Auto-generated method stub
        return "changeit";
    }

    @Override
    public NFUnidadeFederativa getCUF() {
        // TODO Auto-generated method stub
        return NFUnidadeFederativa.valueOf("RN");
    }

    @Override
    public NFTipoEmissao getTipoEmissao() {
        // TODO Auto-generated method stub
        return NFTipoEmissao.EMISSAO_NORMAL;
    }

    @Override
    public String getSSLProtocolo() {
        // TODO Auto-generated method stub
        return "TLSv1";
    }

    @Override
    public Integer getCodigoSegurancaContribuinteID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCodigoSegurancaContribuinte() {
        // TODO Auto-generated method stub
        return null;
    }

}
