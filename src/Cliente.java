/*** Importacões ***/

import java.io.*;
import java.net.*;
import java.util.*;

/*** Classe cliente ***/
public class Cliente
{
    /*** Atributos do cliente
     * socket                       - Socket UDP para conexão.
     * meu_pacote                   - Pacote que será enviado.
     * caminho_origem               - Caminho de origem do arquivo que será enviado.
     * ip                           - Endereço ip do servidor.
     * porta                        - Porta na qual o socket irá mandar o pacote.
     * ID                           - Id do cliente na conexão com o servidor.
     * pacotes                      - Arquivo à ser enviado divido em pacotes.
     * janela                       - Janela para controlar o envio dos pacotes.
     * ultimo_sequencia_enviado     - Representa o ultimo numero de sequencia enviado.
     * ultimo_ack                   - Ultimo ack recebido do servidor.
     * CWND                         - Controla o tamanho da janela.
     * SS_THRESH                    - Controla a partida lenta.
        ***/
    private DatagramSocket socket;
    private Pacote meu_pacote;
    private String caminho_origem;
    private String ip;
    private Integer porta;
    private int ID;
    private ArrayList<Pacote> pacotes;
    private Map<Integer, Estado> janela;
    private int ultimo_sequencia_enviado;
    private int ultimo_ack;
    private Integer CWND = Utils.CWND_inicial;
    private Integer SS_THRESH = Utils.SS_THRESH_inicial;
    private boolean servidor_ativo = true;
    private Timer timer = null;

    /*** Construtor ***/
    private Cliente(String ip, Integer porta, String caminho)
    {
        /*** Inicia os parametros ***/
        this.socket = null;
        this.meu_pacote = null;
        this.caminho_origem = caminho;
        this.ip = ip;
        this.porta = porta;
        this.pacotes = new ArrayList<>();
        /*** Testa se o ip é valido ***/
        try{
            //noinspection unused
            InetAddress teste_de_hostname = InetAddress.getByName(this.ip);
        } catch (UnknownHostException e){
            /*** LOG ***/
            System.out.println(Utils.prefixo_cliente + Utils.hostname_invalido);
            System.exit(0);
        }
        /*** LOG ***/
        System.out.println(Utils.prefixo_cliente + Utils.dados_definidos(this.ip, this.porta, this.caminho_origem));
    }
    /*** Responsavel por fazer o handshake da conexão ***/
    private void handshake(InetAddress IPAddress) throws IOException, ClassNotFoundException {
        reativa_s_timer();
        /*** Esperar o ack do handshake 1 ***/
        boolean checado = false;
        while (!checado)
        {
            verifica_servidor();
            /*** Cria o pacote inicial para o handshake e configura ***/
            Pacote handshake1 = new Pacote();
            handshake1.setSYN(true);
            handshake1.setNumero_de_sequencia(Utils.numero_sequencia_inicial_cliente);
            this.ultimo_sequencia_enviado = Utils.numero_sequencia_inicial_cliente;
            /*** Dados do arquivo para enviar ***/
            byte[] data = Utils.cria_pacote(handshake1);
            /*** Pacote para enviar ***/
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, this.porta);
            /*** Envia o arquivo ***/
            socket.send(sendPacket);
            /*** Log ***/
            System.out.println(Utils.prefixo_cliente + Utils.handshake(1, handshake1));
            verifica_servidor();
                try
                {
                    /*** Array para receber dados do servidor ***/
                    byte[] incomingData = new byte[Utils.tamanho_pacote];
                    /*** Recebe um pacote ***/
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    socket.setSoTimeout(Utils.tempo_espera_ack);
                    socket.receive(incomingPacket);
                    byte[] data2 = incomingPacket.getData();
                    /*** Transforma os bytes em uma instancia de pacote ***/
                    Pacote pacote_ = Utils.cria_pacote(data2, incomingPacket.getLength());
                    verifica_servidor();
                    if(pacote_.isSYN() && pacote_.getNumero_ack() == this.ultimo_sequencia_enviado)
                    {
                        System.out.println(Utils.prefixo_servidor + Utils.handshake(2, pacote_));
                        this.ID = pacote_.getId();
                        checado = true;
                        this.ultimo_ack = pacote_.getNumero_de_sequencia()+1;
                        desativa_s_timer();
                    }
                }catch (SocketTimeoutException e)
                {
                    checado = false;
                    verifica_servidor();
                }
        }
        verifica_servidor();
        /*** Cria o arquivo ***/
        getArquivo();
        boolean checado2 = false;
        /*** O numero de sequencia é o mesmo em todas as tentativas, para que o
         * servidor reconheça um reenvio deste pacote ***/
        this.ultimo_sequencia_enviado += 1;
        while (!checado2)
        {
            meu_pacote = pacotes.get(0);
            /*** Cria a instancia da classe arquivo para o envio ***/
            meu_pacote.setNumero_de_sequencia(this.ultimo_sequencia_enviado);
            meu_pacote.setNumero_ack(this.ultimo_ack);
            meu_pacote.setId((short) this.ID);
            meu_pacote.setACK(true);
            janela.get(0).setNumero_sequencia(this.ultimo_sequencia_enviado);
            /*** Dados do arquivo para enviar ***/
            byte[] data3 = Utils.cria_pacote(meu_pacote);
            /*** Pacote para enviar ***/
            DatagramPacket sendPacket2 = new DatagramPacket(data3, data3.length, IPAddress, this.porta);
            /*** Envia o arquivo ***/
            socket.send(sendPacket2);
            verifica_servidor();
            try
            {
                /*** Log ***/
                System.out.println(Utils.prefixo_cliente + Utils.handshake(3, meu_pacote));
                System.out.println(Utils.prefixo_servidor + Utils.conexao_criada(this.ID));
                byte[] incomingData2 = new byte[Utils.tamanho_pacote];
                /*** Recebe um pacote ***/
                DatagramPacket incomingPacket2 = new DatagramPacket(incomingData2, incomingData2.length);
                socket.setSoTimeout(Utils.tempo_espera_ack);
                socket.receive(incomingPacket2);
                byte[] data4 = incomingPacket2.getData();
                verifica_servidor();
                /*** Transforma os bytes em uma instancia de arquivo ***/
                Pacote pacote_2 = Utils.cria_pacote(data4, incomingPacket2.getLength());
                if(pacote_2.isACK() && pacote_2.getNumero_ack() == this.ultimo_sequencia_enviado )
                {
                    System.out.println(Utils.prefixo_servidor + Utils.pacote_recebido(pacote_2));
                    janela.get(0).setEstado(true);
                    checado2 = true;
                    desativa_s_timer();
                    verifica_servidor();
                }
            }catch (SocketTimeoutException e)
            {
                    checado2 = false;
                    verifica_servidor();
            }
        }
    }

    /*** Envia o arquivo para o servidor ***/
    private void enviar_arquivo()
    {
        try
        {
            verifica_servidor();
            /*** Cria a conexão UDP ***/
            socket = new DatagramSocket();
            /*** Obtem o endereço IP ***/
            InetAddress IPAddress = InetAddress.getByName(this.ip);
            /*** Inicia o handshake ***/
            handshake(IPAddress);
            /*** Cria a instancia da classe arquivo para o envio ***/
            int pacotes_enviados = 1;
            while (pacotes_enviados < pacotes.size())
            {
                verifica_servidor();
                int enviar = this.CWND / Utils.tamanho_util_pacote;
                int enviados = 0;
                int enviei = 0;
                while (enviados < enviar)
                {
                    verifica_servidor();
                    if(pacotes_enviados + enviados >= pacotes.size())   break;
                    meu_pacote = pacotes.get(pacotes_enviados + enviados);
                    meu_pacote.setId((short) this.ID);
                    this.ultimo_sequencia_enviado = Utils.proximo_sequencia(this.ultimo_sequencia_enviado);
                    meu_pacote.setNumero_de_sequencia(this.ultimo_sequencia_enviado);
                    meu_pacote.setNumero_ack(this.ultimo_ack);
                    janela.get(pacotes_enviados + enviados).setNumero_sequencia(this.ultimo_sequencia_enviado);
                    /*** Dados do arquivo para enviar ***/
                    byte[] data = Utils.cria_pacote(meu_pacote);
                    /*** Pacote para enviar ***/
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, this.porta);
                    /*** Envia o arquivo ***/
                    if(!Utils.vai_falhar(Utils.chance_perda_cliente))
                    {
                        socket.send(sendPacket);
                        System.out.println(Utils.prefixo_cliente + Utils.pacote_enviado(meu_pacote));
                    }else
                    {
                        System.out.println(Utils.prefixo_cliente + Utils.pacote_perdido);
                    }
                    janela.get(pacotes_enviados + enviados).ativa_timer();
                    janela.get(pacotes_enviados + enviados).setEstouro(false);
                    enviados++;
                    enviei++;
                    /*** Tempo de espera minimo ***/
                    try
                    {
                        Thread.sleep(Utils.intervalo_envio);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                boolean checkado = false;
                int recebidos = 0;
                while (!checkado)
                {
                    try
                    {
                        verifica_servidor();
                        byte[] incomingData2 = new byte[Utils.tamanho_pacote];
                        /*** Recebe um pacote ***/
                        DatagramPacket incomingPacket2 = new DatagramPacket(incomingData2, incomingData2.length);
                        socket.setSoTimeout(Utils.tempo_espera_ack);
                        socket.receive(incomingPacket2);
                        byte[] data4 = incomingPacket2.getData();
                        /*** Transforma os bytes em uma instancia de arquivo ***/
                        Pacote pacote_2 = Utils.cria_pacote(data4, incomingPacket2.getLength());
                        /*** LOG ***/
                        System.out.println(Utils.janela(enviar, this.CWND, this.SS_THRESH, this.janela));
                        int prox= 0;
                        int indice = 0;
                        for (Map.Entry a : janela.entrySet()){
                            if(!((Estado) a.getValue()).isEstado())
                            {
                                prox = ((Estado) a.getValue()).getNumero_sequencia();
                                indice = (int) a.getKey();
                                break;
                            }
                        }
                        /*** LOG ***/
                        System.out.println(Utils.prefixo_cliente + Utils.seq_esperado(prox,pacote_2.getNumero_ack()));
                        if (pacote_2.isACK() && pacote_2.getNumero_ack() >= prox)
                        {
                            System.out.println(Utils.prefixo_servidor + Utils.pacote_recebido(pacote_2));
                            int contador = obtem_primeiro(pacote_2.getNumero_ack());
                            for(int iterador = indice; iterador <= contador; iterador++)
                            {
                                desativa_s_timer();
                                recebidos++;
                                this.CWND = Utils.proximo_cwnd(this.CWND, this.SS_THRESH);
                            }
                            verifica(contador);
                            this.ultimo_ack = pacote_2.getNumero_de_sequencia()+1;
                        }
                        if (recebidos == enviei) checkado = true;
                    }catch (SocketTimeoutException e){
                        System.out.print(Utils.reenvio_pacote);
                        checkado = true;
                        verifica_servidor();
                    }
                }
                int teste = houve_estouro();
                if (teste != -69)
                {
                    pacotes_enviados = teste;
                    if (teste != 1)
                    {
                        this.SS_THRESH = this.CWND;
                        this.CWND = 512;
                    }
                    this.ultimo_sequencia_enviado = janela.get(teste-1).getNumero_sequencia();
                }
                else pacotes_enviados += enviar;
            }
            terminar_conexao();
            socket.close();
            System.exit(0);
        } catch (SocketException e)
        {
            System.out.println(Utils.prefixo_cliente + Utils.fim_de_conexao);
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    /*** Verifica (Marca como recebido) todos os pacotes até o indice ***/
    private void verifica(int indice)
    {
        for (Map.Entry i: janela.entrySet())
        {
            if((int)i.getKey() < indice && !((Estado) i.getValue()).isEstado())
            {
                janela.get(i.getKey()).setEstouro(false);
                janela.get(i.getKey()).setEstado(true);
                janela.get(i.getKey()).desativa_timer();
            }
        }
        janela.get(indice).setEstouro(false);
        janela.get(indice).setEstado(true);
        janela.get(indice).desativa_timer();
    }
    /*** Obtem a primeira ocorrencia daquele numero de sequencia, cujo Estado = false, e retorna o indice ***/
    private int obtem_primeiro(int numero)
    {
        for (Map.Entry i : janela.entrySet()) if(((Estado)i.getValue()).getNumero_sequencia() == numero && !((Estado) i.getValue()).isEstado())   return (int) i.getKey();
        return 0;
    }
    /*** Percorre o map janela e retorna o indice do primeiro estouro, caso haja algum ***/
    @SuppressWarnings("SuspiciousMethodCalls")
    private int houve_estouro()
    {
        int retorno = 0;
        boolean checar = false;
        for (Map.Entry i: janela.entrySet())
        {
            if (janela.get(i.getKey()).isEstouro())
            {
                janela.get(i.getKey()).setEstouro(false);
                retorno =(int) i.getKey();
                checar = true;
                break;
            }
        }
        for(Map.Entry i2: janela.entrySet()) janela.get(i2.getKey()).setEstouro(false);
        if(checar) return retorno;
        else    return -69;
    }
    /*** Finaliza a conexão, após o arquivo ter sido transferido ***/
    private void terminar_conexao()
    {
        try
        {
            verifica_servidor();
            boolean checkado = false;
            while (!checkado)
            {
                InetAddress IPAddress = InetAddress.getByName(this.ip);
                Pacote terminar_con = new Pacote();
                terminar_con.setFIN(true);
                terminar_con.setId((short)this.ID);
                /*** arrumar ***/
                terminar_con.setNumero_de_sequencia(this.ultimo_sequencia_enviado);
                terminar_con.setNumero_ack(this.ultimo_ack);
                /*** Dados do arquivo para enviar ***/
                byte[] data = Utils.cria_pacote(terminar_con);
                /*** Pacote para enviar ***/
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, this.porta);
                /*** Envia o arquivo ***/
                socket.send(sendPacket);
                /*** Log ***/
                System.out.println(Utils.prefixo_cliente + Utils.pacote_enviado(terminar_con));
                verifica_servidor();
                byte[] incomingData2 = new byte[Utils.tamanho_pacote];
                /*** Recebe um pacote ***/
                DatagramPacket incomingPacket2 = new DatagramPacket(incomingData2, incomingData2.length);
                socket.setSoTimeout(Utils.tempo_espera_finack);
                try
                {
                    socket.receive(incomingPacket2);
                }catch (SocketTimeoutException e)
                {
                    System.out.println(Utils.prefixo_cliente + Utils.finack_perdido);
                    socket.close();
                    System.exit(0);
                }
                byte[] data4 = incomingPacket2.getData();
                /*** Transforma os bytes em uma instancia de arquivo ***/
                Pacote pacote_2 = Utils.cria_pacote(data4, incomingPacket2.getLength());
                verifica_servidor();
                if (pacote_2.isACK() && pacote_2.isFIN())
                {
                    System.out.println(Utils.prefixo_servidor + Utils.pacote_recebido(pacote_2));
                    checkado = true;
                    desativa_s_timer();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean checado2 = false;
        while (!checado2)
        {
            try
            {
                verifica_servidor();
                byte[] incomingData2 = new byte[Utils.tamanho_pacote];
                /*** Recebe um pacote ***/
                DatagramPacket incomingPacket2 = new DatagramPacket(incomingData2, incomingData2.length);
                socket.setSoTimeout(Utils.tempo_espera_ack);
                socket.receive(incomingPacket2);
                byte[] data4 = incomingPacket2.getData();
                /*** Transforma os bytes em uma instancia de arquivo ***/
                Pacote pacote_2 = Utils.cria_pacote(data4, incomingPacket2.getLength());
                verifica_servidor();
                System.out.println(Utils.pacote_recebido(pacote_2));
            if (pacote_2.isFIN())
            {
                verifica_servidor();
                InetAddress IPAddress = InetAddress.getByName(this.ip);
                Pacote terminar_con = new Pacote();
                terminar_con.setFIN(true);
                terminar_con.setACK(true);
                terminar_con.setId((short)this.ID);
                /*** arrumar ***/
                terminar_con.setNumero_de_sequencia(this.ultimo_sequencia_enviado);
                terminar_con.setNumero_ack(this.ultimo_ack);
                /*** Dados do arquivo para enviar ***/
                byte[] data = Utils.cria_pacote(terminar_con);
                /*** Pacote para enviar ***/
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, this.porta);
                /*** Envia o arquivo ***/
                socket.send(sendPacket);
                /*** Log ***/
                System.out.println(Utils.prefixo_cliente + Utils.pacote_enviado(terminar_con));
                System.out.println(Utils.prefixo_cliente + Utils.fim_de_conexao);
                checado2 = true;
                desativa_s_timer();
            }
            }catch (SocketTimeoutException e2)
            {
                checado2 = false;
                verifica_servidor();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }
    private void reativa_s_timer()
    {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                servidor_ativo = false;
            }
        }, Utils.tempo_espera_servidor, Utils.tempo_espera_servidor);
    }
    private void desativa_s_timer()
    {
        this.timer.cancel();
        reativa_s_timer();
    }
    private void verifica_servidor()
    {
        if(!this.servidor_ativo)
        {
            socket.close();
            System.out.println(Utils.prefixo_cliente + Utils.tempo_esgotado);
            System.exit(0);
        }
    }
    /*** Cria uma instancia da classe Pacote para envio ***/
    private void getArquivo()
    {
        /*** Obtem o conteudo do pacote através do diretorio ***/
        File file = new File(caminho_origem);
        if (file.isFile())
        {
            try
            {
                /*** Lê o pacote e põe em um array de bytes ***/
                DataInputStream diStream = new DataInputStream(new FileInputStream(file));
                long len = (int) file.length();
                if (len > Utils.tamanho_maximo_arquivo)
                {
                    System.out.println(Utils.prefixo_cliente + Utils.arquivo_muito_grande);
                    System.exit(0);
                }
                byte[] fileBytes = new byte[(int)len];
                diStream.readFully(fileBytes);
                diStream.close();
                /*** Divide o arquivo em pacotes de até 512 bytes. O ultimo pacote
                 * leva só o resto do arquivo, sem preenchimento ***/
                for (byte[] parte : Utils.divide_em_pacotes(fileBytes))
                {
                    Pacote pacote_ = new Pacote();
                    pacote_.setConteudo(parte);
                    this.pacotes.add(pacote_);
                }
                this.janela = new HashMap<>();
                for (int iterator = 0; iterator < this.pacotes.size(); iterator++)  janela.put(iterator, new Estado());
            } catch (Exception e)
            {
                System.out.println(Utils.prefixo_cliente + Utils.erro_na_leitura);
                System.exit(0);
            }
        } else
        {
            System.out.println(Utils.prefixo_cliente + Utils.arquivo_inexistente);
            System.exit(0);
        }
    }
    /*** Main ***/
    public static void main(String[] args)
    {
        /*** Mensagem de inicio ***/
        System.out.println(Utils.prefixo_cliente + Utils.inicio_cliente);
        System.out.println(Utils.prefixo_cliente + Utils.requisicao_paramentros);
        /*** Obtem os dados de inicio do cliente ***/
        String inicio = new Scanner(System.in).nextLine();
        /*** Verificação incial de quantidade de argumentos ***/
        if(inicio.split(" ").length > 3 || inicio.split(" ").length < 3)
        {
            if(inicio.split(" ").length > 3) System.out.println(Utils.prefixo_cliente + Utils.cliente_muitos_parametros);
            if(inicio.split(" ").length < 3) System.out.println(Utils.prefixo_cliente + Utils.cliente_poucos_parametros);
            System.out.println(Utils.prefixo_cliente + Utils.fim_de_conexao);
            System.exit(0);
        }
        String ip = inicio.split(" ")[0];
        Integer porta = 5555;
        try{
            porta = Integer.parseInt(inicio.split(" ")[1]);
        }catch (NumberFormatException e){
            System.out.println(Utils.prefixo_cliente + Utils.formato_n_errado);
            System.out.println(Utils.prefixo_cliente + Utils.fim_de_conexao);
            System.exit(0);
        }
        String caminho = inicio.split(" ")[2];
        /*** Verificação de porta ***/
        if(porta  < Utils.porta_MIN || porta > Utils.porta_MAX)
        {
            if(porta < Utils.porta_MIN) System.out.println(Utils.prefixo_cliente + Utils.porta_reservada);
            if(porta > Utils.porta_MAX) System.out.println(Utils.prefixo_cliente + Utils.porta_invalida);
            System.out.println(Utils.prefixo_cliente + Utils.fim_de_conexao);
            System.exit(0);
        }
        /*** Criação do cliente ***/
        Cliente cliente = new Cliente(ip, porta, caminho);
        cliente.enviar_arquivo();
    }
}
