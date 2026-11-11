import java.util.Timer;
import java.util.TimerTask;


@SuppressWarnings("SameParameterValue")
public class Estado {

    /*** Atributos  ***/
    /***
     * estado           - Controla se o pacote foi enviado e confirmado com ack.
     * timer            - Timer para estouro.
     * estouro          - Controla se o pacote foi estourado.
     * numero_sequencia - Numero de sequencia atribuido à aquele pacote.
     * ***/
    private boolean estado;
    private Timer timer = null;
    private boolean estouro;
    private int numero_sequencia;
    /*** Construtor ***/
    Estado()
    {
        this.estado = false;
        this.estouro = false;
    }
    void ativa_timer()
    {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                estouro = true;
            }
        }, Utils.tempo_estouro_pacote, Utils.tempo_estouro_pacote);
    }
    void desativa_timer()
    {
        if(this.timer != null) this.timer.cancel();
    }
    /*** Getters e Setters ***/
    int getNumero_sequencia()
    {
        return numero_sequencia;
    }
    void setNumero_sequencia(int numero_sequencia)
    {
        this.numero_sequencia = numero_sequencia;
    }
    boolean isEstado()
    {
        return estado;
    }
    void setEstado(boolean estado)
    {
        this.estado = estado;
    }
    boolean isEstouro()
    {
        return !this.estado && estouro;
    }
    void setEstouro(boolean estouro)
    {
        this.estouro = estouro;
    }
    public String toString()
    {
        return "Estado: "+this.estado + " Estouro: " +this.estouro + " Seq:" +numero_sequencia +" ";
    }
}
