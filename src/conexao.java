import java.util.Timer;
import java.util.TimerTask;

/*** Classe conexão usada no servidor para representa a conexão de um cliente ***/
class conexao
{
    /*** Atributos
     * arquivo          - Arquivo qual o cliente está enviando, (Guarda as partes e monta no fim).
     * timer            - Timer para controlar quando o cliente não está enviando dados.
     * ativa            - Define se a conexão está ativa.
     * prox_esperado    - Proximo numero de sequencia esperado.
     * ***/
    private byte[] arquivo;
    private Timer timer = null;
    private boolean ativa;
    private int prox_esperado;

    /*** Construtor ***/
    conexao()
    {

        this.timer = new Timer();

        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                ativa = false;
            }
        }, Utils.tempo_espera_servidor, Utils.tempo_espera_servidor);

        this.arquivo = null;
        this.ativa = true;
    }

    private void reativa_timer()
    {
        this.timer = new Timer();

        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                ativa = false;
            }
        }, Utils.tempo_espera_servidor, Utils.tempo_espera_servidor);
    }
    void desativa_timer()
    {
        this.timer.cancel();
        reativa_timer();
    }
    /*** Getters e Setters ***/
    boolean isAtiva()
    {
        return ativa;
    }
    int getProx_esperado()
    {
        return prox_esperado;
    }
    void setProx_esperado(int prox_esperado)
    {
        this.prox_esperado = prox_esperado;
    }
    byte[] getArquivo()
    {
        return arquivo;
    }
    void add_parte(byte[] parte)
    {
        if(this.arquivo == null)    this.arquivo = parte;
        else
        {
            byte[] novo = new byte[this.arquivo.length + parte.length];
            System.arraycopy(arquivo, 0, novo, 0, this.arquivo.length);
            int segundo = 0;
            for (int i = this.arquivo.length; i < novo.length; i++)
            {
                novo[i] = parte[segundo];
                segundo++;
            }
            this.arquivo = novo;
        }
    }

}
