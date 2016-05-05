package br.com.ceramic.nfe.classes;

public enum Servico {
	
	NfeConsultaCadastro("NfeConsultaCadastro"),
	NfeConsultaDest("NfeConsultaDest"),
	NfeDownloadNF("NfeDownloadNF"),
	DemaisServicos("DemaisServicos");
	
    private final String codigo;

    private Servico(final String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return this.codigo;
    }

    public static Servico valueOfCodigo(final String codigo) {
        for (Servico servico : Servico.values()) {
            if (servico.getCodigo().equalsIgnoreCase(codigo)) {
                return servico;
            }
        }
        return null;
    }

}
