import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/*** Mensagens uteis no sistema ***/
class Utils {

    /** Constantes **/
    /*** Os tempos e as chances de perda podem mudar com propriedades do Java,
     * por exemplo: java -Dtcpow.perda.cliente=0.5 Cliente. Os testes usam isso. ***/
    static final int tempo_espera_ack = inteiro("tcpow.tempo.ack", 2000);
    static final int numero_de_conexao_inicial = 1;
    static final int tempo_espera_servidor = inteiro("tcpow.tempo.servidor", 10000);
    static final int tamanho_maximo_arquivo = 100000000;
    static final int tempo_verifica_conexoes = 50;
    static final int tamanho_pacote = 524;
    static final int tamanho_util_pacote = 512;
    static final int tamanho_cabecalho = 12;
    static final int porta_MIN = 1023;
    static final int porta_MAX = 65535;
    static final float chance_perda_cliente = probabilidade("tcpow.perda.cliente", 0.2f);
    static final float chance_perda_servidor = probabilidade("tcpow.perda.servidor", 0f);
    static final int CWND_inicial = 512;
    static final int SS_THRESH_inicial = 10000;
    static final int numero_max_seq = 102400;
    static final int numero_sequencia_inicial_cliente = 12345;
    static final int numero_sequencia_inicial_servidor = 4321;
    static final int tempo_espera_finack = inteiro("tcpow.tempo.finack", 2000);
    static final int tempo_estouro_pacote = inteiro("tcpow.tempo.estouro", 1500);
    static final int intervalo_envio = inteiro("tcpow.intervalo.envio", 100);

    /*** Lê um inteiro positivo de uma propriedade do Java, ou usa o padrão ***/
    static int inteiro(String nome, int padrao)
    {
        Integer valor = Integer.getInteger(nome);
        return (valor == null || valor < 0) ? padrao : valor;
    }
    /*** Lê uma probabilidade entre 0 e 1 de uma propriedade do Java, ou usa o padrão ***/
    static float probabilidade(String nome, float padrao)
    {
        String valor = System.getProperty(nome);
        if (valor == null) return padrao;
        try
        {
            float p = Float.parseFloat(valor);
            return (p >= 0 && p <= 1) ? p : padrao;
        } catch (NumberFormatException e)
        {
            return padrao;
        }
    }

    /*** Mensagens do Cliente ***/
    static final String prefixo_cliente = "[CLIENTE]-->";
    static final String inicio_cliente = "Iniciando cliente TCPow.";
    static final String requisicao_paramentros = "Defina os parametros... 'Ip Porta Diretorio-do-arquivo'";
    static final String cliente_muitos_parametros = "O cliente requer somente"
            +" 3 parametros: 'ip porta arquivo'.";
    static final String formato_n_errado = "O parametro 'PORTA' encontrou um erro no dado passado.";
    static final String cliente_poucos_parametros = "O cliente requer obrigatoriamente"
            +" 3 parametros: 'ip porta arquivo'.";
    static final String hostname_invalido = "O hostname passado é invalido.";
    static final String arquivo_inexistente = "O Pacote passado não existe.";
    static final String erro_na_leitura = "Houve um erro na leitura do arquivo";
    static String dados_definidos(String ip, int porta, String caminho)
    {
        return "Dados do cliente definidos: IP = '"+ip +"' Porta = '"
                +porta+"' Pacote = '"+caminho.substring(caminho.lastIndexOf("/") + 1, caminho.length()) +"'.";
    }
    static final String arquivo_muito_grande = "O arquivo escolhido é muito grande, o tamanho maximo é 100MB.";
    static final String tempo_esgotado = "Sem resposta do servidor por 10 segundos.";
    static final String pacote_perdido = "Pacote Perdido no envio.";
    static final String finack_perdido = "FINACK perdido.";
    static String janela(int tamanho, int cwnd, int thresh, Map<Integer, Estado> janela)
    {
        String retorno = "\n\n" +prefixo_cliente +"|- Tamanho da janela = "+tamanho +"\n";
        retorno = retorno.concat(prefixo_cliente +"|- CWND ="+cwnd +" SS_THRESH ="+thresh +"\n");
        retorno = retorno.concat(prefixo_cliente +"|- INICIO JANELA    \n");
        for (Map.Entry i : janela.entrySet()) retorno = retorno.concat(prefixo_cliente +"   |- ("+i.getKey()+") - " +i.getValue().toString()  +"\n");
        retorno = retorno.concat(prefixo_cliente +"|- FIM JANELA \n\n");
        return retorno;
    }
    static String seq_esperado(int esperado, int recebido)
    {
        return "Numero de sequencia esperado -->  " +esperado +" / " + recebido +"  <-- recebido.";
    }

    /*** Mensagens do servidor ***/
    static final String prefixo_servidor = "[SERVIDOR]-->";
    static final String inicio_servidor = "Iniciando servidor TCPow.";
    static final String requisicao_parametros_servidor = "Defina os parametros... 'Porta Diretorio-de-saida'";
    static final String parametro_faltando = "O servidor requer obrigatoriamente " +
            "2 parametros: 'porta diretorio'.";
    static final String muitos_parametros = "O servidor requer somente " +
            "2 parametros: 'porta diretorio'.";
    static final String nome_diretorio_invalido = "O nome do diretorio é invalido, não deve conter"
            +" / \\ | : \" < > ? *";
    static String dados_definidos_servidor(int porta, String diretorio)
    {
        return "Dados do servidor definidos: Porta = '"+porta +"' Diretorio de saida = '/"+diretorio +"/'.";
    }
    static String servidor_iniciado(int porta)
    {
        return "Servidor iniciado na porta " +porta;
    }
    static String conexao_encerrada(int id)
    {
        return "Conexão "+id +"foi encerrada.";
    }
    static String arquivo_salvo(String diretorio)
    {
        return "Arquivo salvo em: " + diretorio + " com sucesso.";
    }
    static String sem_conexao_cliente(int con)
    {
        return "10 segundos sem receber pacotes da conexão " +con +", encerrando conexão.";
    }
    static String prefixo_duplicado = "[DUPLICADO]-->";
    static String rejeitado = "--REJEITADO--";
    static String erro_escrita = "Houve um erro na escrita do arquivo.";
    static String extensao_arquivo = ".file";


    /*** Mensagens gerais ***/
    static final String porta_reservada = "A porta escolhida é reservada ao sistema.";
    static final String porta_invalida = "A porta escolhida é invalida.";
    static final String fim_de_conexao = "Encerrando a conexão";
    static String handshake(int hs, Pacote handshake)
    {
        String retorno = "";
        retorno = retorno.concat("[Handshake "+hs +"° via] SEQ = '" +handshake.getNumero_de_sequencia()
                +"', ACK = '"+handshake.getNumero_ack()
                +"', ID = '"+handshake.getId() +"'");
        if(handshake.isSYN()) retorno = retorno.concat(", SYN");
        if(handshake.isACK()) retorno = retorno.concat(", ACK");
        if(handshake.isFIN()) retorno = retorno.concat(", FIN");
        retorno = retorno.concat(".");
        return retorno;
    }
    static String conexao_criada(int id)
    {
        return "Conexão criada ID = '"+id +"'.";
    }
    static String pacote_enviado(Pacote pacote){
        String retorno = "";
        retorno = retorno.concat("[Pacote enviado]" +" SEQ = '" +pacote.getNumero_de_sequencia()
                +"', ACK = '"+pacote.getNumero_ack()
                +"', ID = '"+pacote.getId() +"'");
        if(pacote.isSYN()) retorno = retorno.concat(", SYN");
        if(pacote.isACK()) retorno = retorno.concat(", ACK");
        if(pacote.isFIN()) retorno = retorno.concat(", FIN");
        retorno = retorno.concat(".");
        return retorno;
    }
    static String pacote_recebido(Pacote pacote)
    {
        String retorno = "";
        retorno = retorno.concat("[Pacote recebido]" +" SEQ = '" +pacote.getNumero_de_sequencia()
                +"', ACK = '"+pacote.getNumero_ack()
                +"', ID = '"+pacote.getId() +"'");
        if(pacote.isSYN()) retorno = retorno.concat(", SYN");
        if(pacote.isACK()) retorno = retorno.concat(", ACK");
        if(pacote.isFIN()) retorno = retorno.concat(", FIN");
        retorno = retorno.concat(".\n");
        return retorno;
    }
    static final String reenvio_pacote = "[REENVIO]-->";

    /*** Funçoes uteis ***/
    static Pacote cria_pacote(byte[] data)
    {
        return cria_pacote(data, data.length);
    }
    /*** Lê um pacote recebido. tamanho é o numero de bytes recebidos (DatagramPacket.getLength) ***/
    static Pacote cria_pacote(byte[] data, int tamanho)
    {
        Pacote retorno = new Pacote();

        byte[] seq = new byte[4];
        System.arraycopy(data, 0, seq, 0, 4);
        ByteBuffer bb = ByteBuffer.wrap(seq);
        retorno.setNumero_de_sequencia(bb.getInt());
        bb.clear();

        byte[] ack_n = new byte[4];
        System.arraycopy(data, 4 , ack_n, 0 , 4);
        bb = ByteBuffer.wrap(ack_n);
        retorno.setNumero_ack(bb.getInt());
        bb.clear();

        byte[] id = new byte[2];
        System.arraycopy(data, 8 , id , 0 , 2);
        bb = ByteBuffer.wrap(id);
        retorno.setId(bb.getShort());
        bb.clear();

        byte[] rest = new byte[2];
        System.arraycopy(data, 10, rest , 0 , 2);

        retorno.setACK(isSet(rest[1], 2));
        retorno.setSYN(isSet(rest[1], 1));
        retorno.setFIN(isSet(rest[1], 0));

        int tamanho_conteudo = Math.max(0, Math.min(tamanho, data.length) - tamanho_cabecalho);
        tamanho_conteudo = Math.min(tamanho_conteudo, tamanho_util_pacote);
        byte[] payload = new byte[tamanho_conteudo];
        System.arraycopy(data, tamanho_cabecalho, payload, 0, tamanho_conteudo);
        retorno.setConteudo(payload);
        bb.clear();

        return retorno;

    }
    static boolean isSet(byte value, int bit)
    {
        return (value&(1<<bit))!=0;
    }
    static byte[] cria_pacote(Pacote pac)
    {
        byte[] seq_n = ByteBuffer.allocate(4).putInt(pac.getNumero_de_sequencia()).array();
        byte[] ack_n = ByteBuffer.allocate(4).putInt(pac.getNumero_ack()).array();
        byte[] id = ByteBuffer.allocate(2).putShort(pac.getId()).array();

        /*** Flags nos 3 ultimos bits do cabeçalho: ACK, SYN e FIN ***/
        byte flags = 0;
        if (pac.isACK()) flags |= 1 << 2;
        if (pac.isSYN()) flags |= 1 << 1;
        if (pac.isFIN()) flags |= 1;

        int tamanho_conteudo = pac.getConteudo() == null ? 0 : Math.min(pac.getConteudo().length, tamanho_util_pacote);
        /*** O pacote tem o tamanho real do conteudo, sem preenchimento ***/
        byte[] data = new byte[tamanho_cabecalho + tamanho_conteudo];
        System.arraycopy(seq_n, 0, data, 0, seq_n.length);
        System.arraycopy(ack_n, 0, data, 4, ack_n.length);
        System.arraycopy(id, 0, data, 8, id.length);
        data[11] = flags;
        if (tamanho_conteudo > 0) System.arraycopy(pac.getConteudo(), 0, data, tamanho_cabecalho, tamanho_conteudo);
        return data;
    }

    /*** Divide um arquivo em partes de no maximo 512 bytes.
     * Um arquivo vazio vira uma parte vazia, porque o handshake leva a primeira parte. ***/
    static List<byte[]> divide_em_pacotes(byte[] arquivo)
    {
        List<byte[]> partes = new ArrayList<>();
        for (int inicio = 0; inicio < arquivo.length; inicio += tamanho_util_pacote)
        {
            int fim = Math.min(inicio + tamanho_util_pacote, arquivo.length);
            byte[] parte = new byte[fim - inicio];
            System.arraycopy(arquivo, inicio, parte, 0, parte.length);
            partes.add(parte);
        }
        if (partes.isEmpty()) partes.add(new byte[0]);
        return partes;
    }

    /*** Novo CWND depois de um ACK: partida lenta abaixo do SS_THRESH, e
     * prevenção de congestionamento (cerca de 1 pacote por RTT) acima dele ***/
    static int proximo_cwnd(int cwnd, int ss_thresh)
    {
        if (cwnd < ss_thresh) return cwnd + tamanho_util_pacote;
        return cwnd + Math.max(1, (tamanho_util_pacote * tamanho_util_pacote) / cwnd);
    }

    /*** Calcula o proximo numero de sequencia a ser usado ***/
    static int proximo_sequencia(int atual)
    {
        if (atual + tamanho_util_pacote > numero_max_seq) return (atual + tamanho_util_pacote) - numero_max_seq;
        else return atual + tamanho_util_pacote;
    }

    /*** Simula a perda de um pacote com a probabilidade passada ***/
    static boolean vai_falhar(float porcentagem)
    {
        return ThreadLocalRandom.current().nextFloat() < porcentagem;
    }

    /*** Testa se o nome do diretorio é valido para ser criado ***/
    static boolean diretorio_valido(String analisar)
    {
        return !(analisar.contains("/") || analisar.contains("\\") || analisar.contains("|")
                || analisar.contains("*") || analisar.contains("?")
                || analisar.contains("\"") || analisar.contains("<")
                || analisar.contains(">") || analisar.contains(":"));
    }

}
