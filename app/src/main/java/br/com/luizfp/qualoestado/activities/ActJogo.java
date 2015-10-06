package br.com.luizfp.qualoestado.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import br.com.luizfp.qualoestado.Constants;
import br.com.luizfp.qualoestado.QualOEstadoApp;
import br.com.luizfp.qualoestado.R;
import br.com.luizfp.qualoestado.app.BackgroundSoundService;
import br.com.luizfp.qualoestado.app.CountAnimation;
import br.com.luizfp.qualoestado.app.MessageBox;
import br.com.luizfp.qualoestado.dialogs.DFBandeira;
import br.com.luizfp.qualoestado.dialogs.DFDescricao;
import br.com.luizfp.qualoestado.dialogs.DFLetra;
import br.com.luizfp.qualoestado.models.Estado;
import br.com.luizfp.qualoestado.models.Gerenciador;
import br.com.luizfp.qualoestado.models.Jogador;
import br.com.luizfp.qualoestado.util.DrawableUtils;
import br.com.luizfp.qualoestado.util.ListUtils;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ActJogo extends BaseActivity {

    private Jogador jogador;
    private EditText edtResposta;
    private SubsamplingScaleImageView imgEstado;
    private Gerenciador gerenciador;
    private Estado estado;
    private int posicaoLista;
    // 26 e não 27 pois a lista começa em 0;
    private static final int QTD_ESTADOS_LISTA = 26;
    private TextView txtPontosJogadorJogo;
    private MediaPlayer mpButtonClick;
    private SharedPreferences sharedPreferences;
    boolean stopMusicService = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_jogo);
        ButterKnife.bind(this);

        setUpToolbar();

        edtResposta = (EditText)findViewById(R.id.edtResposta);
        imgEstado = (SubsamplingScaleImageView)findViewById(R.id.imgEstado);
        txtPontosJogadorJogo = (TextView) findViewById(R.id.txtPontosJogadorJogo);

        //set up button sound
        mpButtonClick = MediaPlayer.create(this, R.raw.button_click);

        edtResposta.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    confirmarResposta();
                    handled = true;
                }
                return handled;
            }
        });

    }


    protected void onResume() {
        super.onResume();
        if (!(((QualOEstadoApp)getApplication()).isTrocaActivity()) &&
                ((QualOEstadoApp)getApplication()).isPlayBgMusic())
            startService(BackgroundSoundService.class);
        gerenciador = gerenciador.getInstance();
        gerenciador.instanciarDAO(this);
        sharedPreferences = getPreferences(MODE_PRIVATE);
        if (sharedPreferences.contains("KEY_STRING")) {
            //TODO melhor fazer depois um metodo retomarJogo no gerenciador que ja cuidara de recriar
            // as dicas com new e todo o resto.
            Type type = new TypeToken<List<Estado>>(){}.getType();
            List<Estado> list = new Gson().fromJson(sharedPreferences.getString("KEY_STRING", null), type);
            posicaoLista = sharedPreferences.getInt("KEY_INT", posicaoLista);
            gerenciador.setListaEstados(list);
            gerenciador.recriarDicas(gerenciador.getListaEstados().get(posicaoLista));
            gerenciador.buscarJogador();
            jogador = gerenciador.getJogador();
        } else {
            gerenciador.iniciarJogo();
            jogador = gerenciador.getJogador();
            posicaoLista = 0;
        }
        txtPontosJogadorJogo.setText(String.valueOf(jogador.getPontos()));
        setMapaOnScreen();

        // Assim caso a pessoa saia do app e torne a jogar novamente, já não encontre e use a mesma
        // lista salva de estados.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stopMusicService)
            stopService(BackgroundSoundService.class);
        ((QualOEstadoApp)getApplication()).setTrocaActivity(false);
        sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("KEY_STRING", new Gson().toJson(gerenciador.getListaEstados()));
        // tem que passar posicaoLista - 1 pois a variavel ja foi incremetada para o indice do
        // próximo mapa no método setMapaOnScreen
        editor.putInt("KEY_INT", posicaoLista - 1);
        editor.commit();
    }

    private void setMapaOnScreen() {
        if (posicaoLista == QTD_ESTADOS_LISTA) {
            // Se jogou todos os estados da lista, reordenar a lista e começar a mostrar novamente
            gerenciador.setListaEstados(ListUtils.shuffleList(gerenciador.getListaEstados()));
            posicaoLista = 0;
        }
        estado = gerenciador.getListaEstados().get(posicaoLista);
        gerenciador.recriarDicas(estado);
        String nomeImgMapa = estado.getNomeImgMapa();
        int resId = DrawableUtils.getImageIdByName(nomeImgMapa, this);
        //imgEstado.setImageResource(resId);
        imgEstado.setImage(ImageSource.resource(resId));
        posicaoLista++;
    }

    @OnClick(R.id.btnConfirmarResposta) void confirmarResposta() {
        if (((QualOEstadoApp)getApplication()).isPlayButtonSound())
            mpButtonClick.start();
        confirmaResposta();
    }

    private void confirmaResposta() {
        int novosPontos;
        String resposta = edtResposta.getText().toString().trim();
        if (resposta.isEmpty())
            toast(getString(R.string.label_campo_resposta_vazio));
        else if (gerenciador.confirmaJogada(resposta, estado.getNome(), estado.getSigla())) {
            gerenciador.resetarUsoDicas();
            novosPontos = jogador.getPontos() + Constants.VALOR_ACERTAR_RESPOSTA;
            CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
            jogador.setPontos(novosPontos);
            jogador.setNumAcertos(jogador.getNumAcertos() + 1);
            gerenciador.acertouJogada(jogador);
            toast(getString(R.string.label_resposta_correta));
            edtResposta.setText("");
            verificaMaiorNumPontos();
            setMapaOnScreen();
        } else {
            toast(getString(R.string.label_resposta_incorreta));
            if (jogador.getPontos() > 0) {
                novosPontos = jogador.getPontos() - Constants.CUSTO_ERRAR_RESPOSTA;
                CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
                jogador.setPontos(novosPontos);
                verificaMenorNumPontos();
            }
            jogador.setNumErros(jogador.getNumErros() + 1);
            gerenciador.errouJogada(jogador);
        }
    }

    @OnClick(R.id.btnPularEstado) void pularEstado() {
        if (((QualOEstadoApp)getApplication()).isPlayButtonSound())
            mpButtonClick.start();
        if (jogador.getPontos() >= Constants.CUSTO_PULAR_RESPOSTA) {
            MessageBox.showAlertaGastoPontos(this, "Pular um estado custa " +
                    Constants.CUSTO_PULAR_RESPOSTA + " pontos. Você tem certeza que deseja pular?")
                    .setPositiveButton(getString(R.string.label_ok), positiveActionPular())
                    .show();
        } else
            MessageBox.show(this, "", getString(R.string.label_sem_pontos));

    }

    private DialogInterface.OnClickListener positiveActionPular() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                gerenciador.resetarUsoDicas();
                int novosPontos = jogador.getPontos() - Constants.CUSTO_PULAR_RESPOSTA;
                CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
                jogador.setPontos(novosPontos);
                jogador.setNumPulosResposta(jogador.getNumPulosResposta() + 1);
                gerenciador.pulouJogada(jogador);
                edtResposta.setText("");
                verificaMenorNumPontos();
                setMapaOnScreen();
            }
        };
    }

    public void getDicaBandeira(View view) {
        if (!gerenciador.getDicaBandeira().isJaComprada()) {
            if (jogador.getPontos() >= Constants.CUSTO_DICA_BANDEIRA) {
                MessageBox.showAlertaGastoPontos(this, "Comprar uma Dica Bandeira custa " +
                        Constants.CUSTO_DICA_BANDEIRA + " pontos. Você tem certeza que deseja comprar?")
                        .setPositiveButton(getString(R.string.label_ok), positiveActionDicaBandeira())
                        .show();
            } else
                MessageBox.show(this, "", getString(R.string.label_sem_pontos));
        } else {
            showDicaBandeira();
        }
    }

    private DialogInterface.OnClickListener positiveActionDicaBandeira() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int novosPontos = jogador.getPontos() - gerenciador.getDicaBandeira().getCustoEmPontos();
                CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
                jogador.setPontos(novosPontos);
                jogador.setNumUsosDicaBandeira(jogador.getNumUsosDicaBandeira() + 1);
                gerenciador.usouDicaBandeira(jogador);
                gerenciador.getDicaBandeira().setJaComprada(true);
                verificaMenorNumPontos();
                showDicaBandeira();
            }
        };
    }

    private void showDicaBandeira() {
        DFBandeira dfBandeira = DFBandeira.newInstance(gerenciador.getDicaBandeira());
        dfBandeira.show(getSupportFragmentManager(), "TAG");
    }

    public void getDicaDescricao(View view) {
        if (!gerenciador.getDicaDescricao().isJaComprada()) {
            if (jogador.getPontos() >= Constants.CUSTO_DICA_DESCRICAO) {
               MessageBox.showAlertaGastoPontos(this, "Comprar uma Dica Descrição custa " +
                       Constants.CUSTO_DICA_DESCRICAO + " pontos. Você tem certeza que deseja comprar?")
                       .setPositiveButton(getString(R.string.label_ok), positiveActionDicaDescricao())
                       .show();
            } else
                MessageBox.show(this, "", getString(R.string.label_sem_pontos));
        } else {
            showDicaDescricao();
        }
    }

    private DialogInterface.OnClickListener positiveActionDicaDescricao() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int novosPontos = jogador.getPontos() - gerenciador.getDicaDescricao().getCustoEmPontos();
                CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
                jogador.setPontos(novosPontos);
                jogador.setNumUsosDicaDescricao(jogador.getNumUsosDicaDescricao() + 1);
                gerenciador.usouDicaDescricao(jogador);
                gerenciador.getDicaDescricao().setJaComprada(true);
                verificaMenorNumPontos();
                showDicaDescricao();
            }
        };
    }

    private void showDicaDescricao() {
        DFDescricao dfDescricao = DFDescricao.newInstance(gerenciador.getDicaDescricao());
        dfDescricao.show(getSupportFragmentManager(), "TAG");
    }

    public void getDicaLetra(View view) {
        if (!gerenciador.getDicaLetra().isJaComprada()) {
            if (jogador.getPontos() >= Constants.CUSTO_DICA_LETRA) {
                MessageBox.showAlertaGastoPontos(this, "Comprar uma Dica Letra custa " +
                        Constants.CUSTO_DICA_LETRA + " pontos. Você tem certeza que deseja comprar?")
                        .setPositiveButton(getString(R.string.label_ok), positiveActionDicaLetra())
                        .show();
            } else
                MessageBox.show(this, "", getString(R.string.label_sem_pontos));
        } else {
            showDicaLetra();
        }
    }

    private DialogInterface.OnClickListener positiveActionDicaLetra() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int novosPontos = jogador.getPontos() - gerenciador.getDicaLetra().getCustoEmPontos();
                CountAnimation.startCountAnimation(jogador.getPontos(), novosPontos, txtPontosJogadorJogo, 500);
                jogador.setPontos(novosPontos);
                jogador.setNumUsosDicaLetra(jogador.getNumUsosDicaLetra() + 1);
                gerenciador.usouDicaLetra(jogador);
                gerenciador.getDicaLetra().setJaComprada(true);
                verificaMenorNumPontos();
                showDicaLetra();
            }
        };
    }

    private void showDicaLetra() {
        DFLetra dfLetra = DFLetra.newInstance(gerenciador.getDicaLetra());
        dfLetra.show(getSupportFragmentManager(), "TAG");
    }

    private void verificaMaiorNumPontos() {
        if (jogador.getPontos() > jogador.getMaiorNumPontos())
            jogador.setMaiorNumPontos(jogador.getPontos());
    }

    private void verificaMenorNumPontos() {
        if (jogador.getPontos() < jogador.getMenorNumPontos())
            jogador.setMenorNumPontos(jogador.getPontos());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopMusicService = false;
        // assim gerenciador sempre referencia o jogador com os atributos mais novos
        gerenciador.setJogador(jogador);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_act_jogo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == android.R.id.home) {
            stopMusicService = false;
            // assim gerenciador sempre referencia o jogador com os atributos mais novos
            gerenciador.setJogador(jogador);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
