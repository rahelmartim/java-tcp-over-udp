import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

public class Servidor extends Thread
{
    /*** Atributos do servidor
     * NUMERO_CONEXAO               - Controla o proximo numero para conexão disponivel.
     * conexoes                     - Mapeia as conexões estabeçecidas.
     * socket                       - Socket para conexão.
     * pacote                       - Pacote recebido.
     * porta                        - Porta em que o servidor vai escutar.
     * Diretorio onde o servidor irá salvar os arquivos.
     * incomingPacket               - Pacote recebido do lado cliente.
     * ***/
    private static int NUMERO_CONEXAO;
    private static Map<Integer, conexao> conexoes;
    private static DatagramSocket socket;
    private static String diretorio_saida;
    private Pacote pacote;
    private DatagramPacket incomingPacket;

    /*** Cria o pacote no lado servidor ***/
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void cria_arquivo()
    {
        /*** Diretorio para saida do pacote ***/
        String diretorio = diretorio_saida + "/" + pacote.getId() +Utils.extensao_arquivo;
        /*** Verifica se o diretorio existe ***/
        if (!new File(diretorio_saida).exists()) new File(diretorio_saida).mkdirs();

        /*** Cria o pacote para escrita ***/
        File dstFile = new File(diretorio);
        FileOutputStream fileOutputStream;
        /*** Escreve o conteudo ***/
        try
        {
            fileOutputStream = new FileOutputStream(dstFile);
            fileOutputStream.write(conexoes.get((int)pacote.getId()).getArquivo());
            fileOutputStream.flush();
            fileOutputStream.close();
            System.out.println(Utils.prefixo_servidor + Utils.arquivo_salvo(diretorio));
        } catch (IOException e)
        {
            System.out.println(Utils.prefixo_servidor + Utils.erro_escrita);
        }
    }
    /*** Checa se alguma conexão passou 10 segundos sem se comunicar e fecha ***/
    @SuppressWarnings("SuspiciousMethodCalls")
    private static void verifica_conexoes(){
        for (Map.Entry entry: conexoes.entrySet() )
        {
            if(!((conexao) entry.getValue()).isAtiva())
            {
                conexoes.remove(entry.getKey());
                System.out.println(Utils.prefixo_servidor + Utils.sem_conexao_cliente((int)entry.getKey()));
            }
        }
    }
    /*** Construtor passando o pacote para a thread ***/
    private Servidor(DatagramPacket _incomingPacket)
    {
        this.incomingPacket = _incomingPacket;
    }
    /*** Main ***/
    public static void main(String[] args)
    {
        NUMERO_CONEXAO = Utils.numero_de_conexao_inicial;
        conexoes = new java.util.concurrent.ConcurrentHashMap<>();
        try
        {
            /*** LOG de inicio do servidor ***/
            System.out.println(Utils.prefixo_servidor + Utils.inicio_servidor);
            System.out.println(Utils.prefixo_servidor + Utils.requisicao_parametros_servidor);
            /*** Obtem os dados de inicio do servidor ***/
            String inicio = new Scanner(System.in).nextLine();
            if(inicio.split(" ").length < 2){
                System.out.println(Utils.prefixo_servidor + Utils.parametro_faltando);
                System.exit(0);
            }
            Integer _porta = Integer.parseInt(inicio.split(" ")[0]);
            String _diretorio_saida = inicio.split(" ")[1];
            /*** Verificação de erros na inicialização do servidor ***/
            if(_porta  < Utils.porta_MIN || _porta > Utils.porta_MAX || inicio.split(" ").length > 2 || !Utils.diretorio_valido(_diretorio_saida))
            {
                if(_porta < Utils.porta_MIN) System.out.println(Utils.prefixo_servidor + Utils.porta_reservada);
                else if(_porta > Utils.porta_MAX) System.out.println(Utils.prefixo_servidor + Utils.porta_invalida);
                else if(inicio.split(" ").length > 2) System.out.println(Utils.prefixo_servidor + Utils.muitos_parametros);
                else System.out.println(Utils.prefixo_servidor + Utils.nome_diretorio_invalido);
                System.out.println(Utils.prefixo_servidor + Utils.fim_de_conexao);
                System.exit(0);
            }
            /*** Inicia o servidor ***/
            /*** Cria o socket UDP da conexão ***/
            diretorio_saida = _diretorio_saida;
            socket = new DatagramSocket(_porta);
            /*** Log ***/
            System.out.println(Utils.prefixo_servidor + Utils.dados_definidos_servidor(_porta, diretorio_saida));
            System.out.println(Utils.prefixo_servidor + Utils.servidor_iniciado(_porta));
            /*** Recebe os pacotes ***/
            //noinspection InfiniteLoopStatement
            while (true)
            {
                /*** Checa se alguma conexão expirou***/
                verifica_conexoes();
                /*** Recebe um pacote. Cada pacote tem o seu proprio buffer, porque
                 * a thread que o trata pode rodar enquanto o proximo pacote chega ***/
                byte[] incomingData = new byte[Utils.tamanho_pacote * 2];
                DatagramPacket incomingPacket_ = new DatagramPacket(incomingData, incomingData.length);
                socket.setSoTimeout(Utils.tempo_verifica_conexoes);
                try
                {
                    socket.receive(incomingPacket_);
                }catch (SocketTimeoutException e){
                    continue;
                }
                /*** Inicia uma thread para a conexão ***/
                Thread thread_connection = new Servidor(incomingPacket_);
                thread_connection.start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }catch (ArrayIndexOutOfBoundsException e)
        {
            System.out.println(Utils.prefixo_servidor + Utils.parametro_faltando);
        }catch (NumberFormatException e)
        {
            System.out.println(Utils.prefixo_servidor + Utils.porta_invalida);
        }
    }
    /*** Metodo da thread ***/
    @Override
    public void run()
    {
        try
        {
            byte[] data = this.incomingPacket.getData();
            /*** Transforma os bytes em uma instancia de pacote ***/
            this.pacote = Utils.cria_pacote(data, this.incomingPacket.getLength());
            /*** Obtem o endereço/porta do cliente ***/
            InetAddress IPAddress = this.incomingPacket.getAddress();
            int port = this.incomingPacket.getPort();
            /*** Se o pacote for de inicio de conexão ***/
            if(pacote.isSYN())
            {
              /*** Um cliente por vez recebe um ID de conexão ***/
              synchronized (conexoes)
              {
                /*** Cria um pacote de resposta ***/
                Pacote handshake2 = new Pacote();
                handshake2.setNumero_de_sequencia(Utils.numero_sequencia_inicial_servidor);
                handshake2.setNumero_ack(pacote.getNumero_de_sequencia());
                handshake2.setACK(true);
                handshake2.setSYN(true);
                /*** Seta o id que ele pode usar ***/
                handshake2.setId((short) NUMERO_CONEXAO);
                /*** Log ***/
                System.out.println(Utils.prefixo_cliente + Utils.handshake(1, pacote));
                /*** Dados do pacote para enviar ***/
                byte[] data2 = Utils.cria_pacote(handshake2);
                /*** Pacote para enviar ***/
                DatagramPacket sendPacket = new DatagramPacket(data2, data2.length, IPAddress, port);
                /*** Envia o pacote ***/
                if(!Utils.vai_falhar(Utils.chance_perda_servidor))
                {
                    socket.send(sendPacket);
                    System.out.println(Utils.prefixo_servidor + Utils.handshake(2, handshake2));
                    System.out.println(Utils.prefixo_servidor + Utils.conexao_criada((int) pacote.getId() +1));
                    conexao cliente_con = new conexao();
                    cliente_con.setProx_esperado(pacote.getNumero_de_sequencia()+1);
                    conexoes.put(NUMERO_CONEXAO, cliente_con);
                    NUMERO_CONEXAO++;
                }else System.out.println(Utils.prefixo_servidor + Utils.pacote_perdido);
              }

            }else{
            /*** Cria o pacote no lado servidor ***/
                /*** Se não for um pacote de finalizar conexão, é um de dados ***/
                conexao con = conexoes.get((int) pacote.getId());
                if (con == null)
                {
                    /*** Pacote de uma conexão que não existe mais: descarta ***/
                    return;
                }
                /*** Um pacote por vez em cada conexão. Sem isso, um pacote e o seu
                 * reenvio podem ser aceitos ao mesmo tempo por duas threads ***/
                synchronized (con)
                {
                if(!pacote.isFIN())
                {
                    /*** Checa se é o esperado
                     * Se não for, ele apenas descarta ***/
                    if(conexoes.get((int)pacote.getId()).getProx_esperado() != pacote.getNumero_de_sequencia())
                    {
                        conexoes.get((int)pacote.getId()).desativa_timer();
                        Pacote pacote_r = new Pacote();
                        pacote_r.setACK(true);
                        pacote_r.setNumero_ack(conexoes.get((int)pacote.getId()).getProx_esperado() - 512);
                        pacote_r.setNumero_de_sequencia(pacote.getNumero_ack());
                        pacote_r.setId(pacote.getId());
                        /*** Dados do pacote para enviar ***/
                        byte[] data2 = Utils.cria_pacote(pacote_r);
                        /*** Pacote para enviar ***/
                        DatagramPacket sendPacket = new DatagramPacket(data2, data2.length, IPAddress, port);
                        /*** Envia o pacote ***/
                        socket.send(sendPacket);
                        System.out.println(Utils.prefixo_servidor + Utils.pacote_enviado(pacote_r));
                        /*** Log ***/
                        System.out.println(Utils.prefixo_duplicado + Utils.pacote_recebido(pacote) + Utils.rejeitado);
                    }else
                    {
                        /*** Se for o esperado ***/
                        conexoes.get((int)pacote.getId()).add_parte(pacote.getConteudo());
                        conexoes.get((int)pacote.getId()).desativa_timer();
                        conexoes.get((int)pacote.getId()).setProx_esperado(Utils.proximo_sequencia(pacote.getNumero_de_sequencia()));
                        Pacote pacote_r = new Pacote();
                        pacote_r.setACK(true);
                        pacote_r.setNumero_ack(pacote.getNumero_de_sequencia());
                        pacote_r.setNumero_de_sequencia(pacote.getNumero_ack());
                        pacote_r.setId(pacote.getId());
                        /*** Dados do pacote para enviar ***/
                        byte[] data2 = Utils.cria_pacote(pacote_r);
                        /*** Pacote para enviar ***/
                        DatagramPacket sendPacket = new DatagramPacket(data2, data2.length, IPAddress, port);
                        /*** Envia o pacote ***/
                        /*** Há uma chance de perder o pacote ***/
                        if(!Utils.vai_falhar(Utils.chance_perda_servidor))
                        {
                            socket.send(sendPacket);
                            System.out.println(Utils.prefixo_servidor + Utils.pacote_enviado(pacote_r));
                        }else System.out.println(Utils.prefixo_servidor + Utils.pacote_perdido);
                    }
                    /*** Se for um pacote de fim de conexão ***/
                }else if(pacote.isFIN())
                {
                    /*** Cria o arquivo no lado servidor ***/
                    /*** Envia um pacote de confirmação ***/
                    if (!pacote.isACK()) {
                    cria_arquivo();
                    Pacote terminar_con = new Pacote();
                    terminar_con.setNumero_ack(conexoes.get((int)pacote.getId()).getProx_esperado() - 512);
                    terminar_con.setNumero_de_sequencia(pacote.getNumero_ack());
                    terminar_con.setId(pacote.getId());
                    terminar_con.setFIN(true);
                    terminar_con.setACK(true);
                    /*** Dados do pacote para enviar ***/
                    byte[] data3 = Utils.cria_pacote(terminar_con);
                    /*** Pacote para enviar ***/
                    DatagramPacket sendPacket2 = new DatagramPacket(data3, data3.length, IPAddress, port);
                    /*** Envia o pacote ***/
                    socket.send(sendPacket2);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Pacote terminar_con2 = new Pacote();
                    terminar_con2.setNumero_ack(conexoes.get((int)pacote.getId()).getProx_esperado() - 512);
                    terminar_con2.setNumero_de_sequencia(pacote.getNumero_ack());
                    terminar_con2.setId(pacote.getId());
                    terminar_con2.setFIN(true);
                    terminar_con2.setACK(false);
                    /*** Dados do pacote para enviar ***/
                    byte[] data4 = Utils.cria_pacote(terminar_con2);
                    /*** Pacote para enviar ***/
                    DatagramPacket sendPacket3 = new DatagramPacket(data4, data4.length, IPAddress, port);
                    /*** Envia o pacote ***/
                    socket.send(sendPacket3);
                    }else{
                        conexoes.remove((int) pacote.getId());
                        System.out.println(Utils.prefixo_servidor + Utils.conexao_encerrada((int)pacote.getId()));
                    }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
