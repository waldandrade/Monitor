# NFeMonitor
Ferramenta para monitorar pastas do Google Drive afim de executar as funções de nota fiscal eletrônica.


Atualmente, para que o programa funcione ... basta 


1. criar uma pasta .nfe na raiz do google drive
2. Criar uma pasta "send" e outra pasta "finalized" dentro da pasta .nfe
3. Altere a implementação de NFeConfig para suas necessidades.
4. Executar o arquivo br.com.ceramic.assinador.util.Main e inserir a senha do PIN (certificado A3)

Atualmente o projeto é voltado para o certificado A3, mas alterando o NFeConfig é possível mudar.
